"""
Convierte el GTFS oficial de TransMilenio en el archivo compacto que la app lleva en
app/src/main/assets/troncal.txt (solo el componente troncal, agency_id 1).

Fuentes (datos abiertos de TransMilenio S.A.):
  - GTFS estático: https://datosabiertos-transmilenio.hub.arcgis.com  ("GTFS Estáticos AAAA-MM-DD")
  - Estaciones troncales con su troncal (letra y nombre): capa 2 y 5 del FeatureServer
    Consulta_Planificacion_SITP en gis.transmilenio.gov.co

Uso (desde la raíz del proyecto):
  python tools/gtfs/exportar_troncal.py                       # descarga lo más reciente
  python tools/gtfs/exportar_troncal.py --gtfs GTFS_20260929.zip
"""

import argparse
import collections
import csv
import datetime
import io
import json
import math
import os
import re
import statistics
import tempfile
import urllib.request
import zipfile

HUB = "https://datosabiertos-transmilenio.hub.arcgis.com/api/search/v1/collections/all/items"
ITEM = "https://www.arcgis.com/sharing/rest/content/items/{}?f=json"
CAPA = (
    "https://gis.transmilenio.gov.co/arcgis/rest/services/ConsultaSubgerenciaPlanificacionSITP/"
    "Consulta_Planificacion_SITP/FeatureServer/{}/query?where=1%3D1&outFields=*&returnGeometry=false&f=json"
)
AGENCIA_TRONCAL = "1"
# Los tiempos de un mismo patrón cambian con la hora del día; se agrupan en perfiles de 3 min
CUBETA_SEG = 180


def leer_json(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)


def gtfs_mas_reciente():
    items = leer_json(HUB + "?q=GTFS&limit=100")["features"]
    titulos = [f for f in items if f["properties"]["title"].startswith("GTFS Estáticos")]
    ultimo = max(titulos, key=lambda f: f["properties"]["title"])
    url = leer_json(ITEM.format(ultimo["id"]))["url"]
    destino = os.path.join(tempfile.mkdtemp(prefix="gtfs_"), os.path.basename(url))
    print("Descargando", url)
    urllib.request.urlretrieve(url, destino)
    return destino


def filas(z, nombre):
    return csv.DictReader(io.TextIOWrapper(z.open(nombre), encoding="utf-8-sig"))


def segundos(hora):
    h, m, s = map(int, hora.split(":"))
    return h * 3600 + m * 60 + s


def limpiar(nombre):
    return re.sub(r"\s+", " ", nombre).strip()


# "Calle 100 C - 2 ó 5" -> "C:2 ó 5" (vagón y puertas); los sufijos como "-nw" o "-T" se ignoran.
# "Portal Norte T1" / "Portal 80 T-2" / "Portal Américas T2 A" -> "T1" / "T2" / "T2A"
VAGON = re.compile(r"\s([A-Z])\s*-?\s*(\d+(?:\s*(?:ó|o|y|,)\s*\d+)*)")
PLATAFORMA = re.compile(r"\sT-?(\d+)\s?([A-Z]?)\b")

# Los nombres de la capa de GIS vienen sin tildes
TILDES = {"Americas": "Américas"}


def anden(nombre_parada):
    m = PLATAFORMA.search(nombre_parada)
    if m:
        return f"T{m.group(1)}{m.group(2)}"
    vagones = list(VAGON.finditer(nombre_parada))
    if vagones:
        m = vagones[-1]
        return f"{m.group(1)}:{re.sub(r'\s+', ' ', m.group(2))}"
    return ""


def metros(lat1, lon1, lat2, lon2):
    x = math.radians(lon2 - lon1) * math.cos(math.radians((lat1 + lat2) / 2))
    return 6_371_000 * math.hypot(x, math.radians(lat2 - lat1))


def mediana_entera(valores):
    return int(round(statistics.median(valores)))


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--gtfs", help="zip del GTFS; si falta, se descarga el más reciente")
    p.add_argument("--salida", default="app/src/main/assets/troncal.txt")
    args = p.parse_args()

    ruta_gtfs = args.gtfs or gtfs_mas_reciente()
    z = zipfile.ZipFile(ruta_gtfs)

    # Letra y nombre oficial de cada troncal, y a qué troncal pertenece cada estación
    trazados = {f["attributes"]["id_trazado"]: f["attributes"] for f in leer_json(CAPA.format(5))["features"]}
    troncal_de = {}
    oficiales = []
    for f in leer_json(CAPA.format(2))["features"]:
        a = f["attributes"]
        t = trazados.get(a["id_trazado"])
        if t and a["num_est"]:
            troncal_de[int(a["num_est"])] = t["le_troncal"]
            oficiales.append((a["latitud"], a["longitud"], t["le_troncal"], a["nom_est"]))
    nombres_troncal = {t["le_troncal"]: TILDES.get(t["nom_tronc"], t["nom_tronc"]) for t in trazados.values()}

    rutas = {r["route_id"]: r for r in filas(z, "routes.txt") if r["agency_id"] == AGENCIA_TRONCAL}
    viajes = {v["trip_id"]: v for v in filas(z, "trips.txt") if v["route_id"] in rutas}
    paradas = {s["stop_id"]: s for s in filas(z, "stops.txt")}

    secuencias = collections.defaultdict(list)
    for st in filas(z, "stop_times.txt"):
        if st["trip_id"] in viajes:
            secuencias[st["trip_id"]].append((int(st["stop_sequence"]), st["stop_id"], segundos(st["arrival_time"])))

    # Patrón = misma ruta y mismas paradas (andenes) en el mismo orden
    patrones = collections.defaultdict(list)
    for trip_id, seq in secuencias.items():
        seq.sort()
        clave = (rutas[viajes[trip_id]["route_id"]]["route_short_name"], tuple(s for _, s, _ in seq))
        inicio = seq[0][2]
        patrones[clave].append((viajes[trip_id]["service_id"], inicio, [t - inicio for _, _, t in seq]))

    # Estaciones: el padre (location_type 1) de cada andén usado
    ids_estacion = sorted({paradas[s]["parent_station"] for _, ps in patrones for s in ps})
    indice = {e: i for i, e in enumerate(ids_estacion)}
    # Algunas estaciones nuevas tienen otro código en el GTFS (90009 vs 5011): se emparejan con la
    # estación oficial más cercana si está a menos de 500 m
    por_cercania = []
    estaciones = []
    for e in ids_estacion:
        s = paradas[e]
        lat, lon = float(s["stop_lat"]), float(s["stop_lon"])
        letra = troncal_de.get(int(e))
        if letra is None:
            d, la, lo, letra, nombre = min((metros(lat, lon, la, lo), la, lo, le, n) for la, lo, le, n in oficiales)
            if d > 500:
                raise SystemExit(f"Sin troncal para {s['stop_name']} (la oficial más cercana, {nombre}, está a {d:.0f} m)")
            por_cercania.append(f"{limpiar(s['stop_name'])} -> {nombre} ({letra}, {d:.0f} m)")
        estaciones.append([limpiar(s["stop_name"]), lat, lon, "0" if s["wheelchair_boarding"] == "2" else "1", letra])

    servicios_usados = {sv for v in patrones.values() for sv, _, _ in v}
    calendario = {c["service_id"]: c for c in filas(z, "calendar.txt")}
    excepciones = collections.defaultdict(lambda: ([], []))
    for c in filas(z, "calendar_dates.txt"):
        excepciones[c["service_id"]][0 if c["exception_type"] == "1" else 1].append(c["date"])
    info = next(filas(z, "feed_info.txt"))
    tarifa = next(filas(z, "fare_attributes.txt"))["price"]

    lineas = [
        "ROUTIFY|1",
        f"FUENTE|{os.path.basename(ruta_gtfs)}|{info['feed_start_date']}|{info['feed_end_date']}|"
        f"{datetime.date.today():%Y%m%d}",
        f"TARIFA|{int(float(tarifa))}",
    ]
    dias = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]
    for sv in sorted(servicios_usados):
        c = calendario[sv]
        mas, menos = excepciones[sv]
        lineas.append(f"SERVICIO|{sv}|{''.join(c[d] for d in dias)}|{c['start_date']}|{c['end_date']}|"
                      f"{','.join(mas)}|{','.join(menos)}")
    for letra in sorted({e[4] for e in estaciones}):
        lineas.append(f"TRONCAL|{letra}|{nombres_troncal.get(letra, letra)}")
    for nombre, lat, lon, accesible, letra in estaciones:
        lineas.append(f"ESTACION|{nombre}|{lat:.6f}|{lon:.6f}|{accesible}|{letra}")

    sin_anden = collections.Counter()
    total_perfiles = 0
    for (codigo, ps), lista in sorted(patrones.items()):
        andenes = [anden(limpiar(paradas[s]["stop_name"])) for s in ps]
        sin_anden.update(limpiar(paradas[s]["stop_name"]) for s, a in zip(ps, andenes) if not a)
        lineas.append(f"PATRON|{codigo}|{','.join(str(indice[paradas[s]['parent_station']]) for s in ps)}|"
                      f"{';'.join(andenes)}")
        cubetas = collections.defaultdict(list)
        for _, _, offsets in lista:
            cubetas[round(offsets[-1] / CUBETA_SEG)].append(offsets)
        perfil_de = {}
        for n, (cubeta, grupo) in enumerate(sorted(cubetas.items())):
            perfil = [mediana_entera(col) for col in zip(*grupo)]
            for i in range(1, len(perfil)):
                perfil[i] = max(perfil[i], perfil[i - 1])
            lineas.append(f"PERFIL|{','.join(map(str, perfil))}")
            perfil_de[cubeta] = n
        total_perfiles += len(cubetas)
        por_servicio = collections.defaultdict(list)
        for sv, inicio, offsets in lista:
            por_servicio[sv].append((inicio, perfil_de[round(offsets[-1] / CUBETA_SEG)]))
        for sv, salidas in sorted(por_servicio.items()):
            salidas.sort()
            lineas.append(f"VIAJES|{sv}|{','.join(f'{s}.{p}' for s, p in salidas)}")

    os.makedirs(os.path.dirname(args.salida), exist_ok=True)
    with open(args.salida, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lineas) + "\n")

    print(f"{args.salida}: {os.path.getsize(args.salida) / 1024:.0f} KB")
    print(f"  {len(estaciones)} estaciones, {len(patrones)} patrones, {total_perfiles} perfiles, "
          f"{len(secuencias)} viajes, servicios {sorted(servicios_usados)}")
    print(f"  vigencia {info['feed_start_date']}-{info['feed_end_date']}, tarifa {tarifa}")
    for linea in por_cercania:
        print("  troncal por cercanía:", linea)
    if sin_anden:
        print("  andenes sin vagón reconocible:", sorted(sin_anden))


if __name__ == "__main__":
    main()
