"""
Teste de estresse do cache-aside de cartas (Quarkus + Redis + Magic: The Gathering API).

Executa duas fases com paralelismo (ThreadPoolExecutor):

  Fase 1 — populate : busca 15 cartas em paralelo → espera-se X-Cache: MISS
                      (cada miss consulta a API externa e grava no Redis)
  Fase 2 — cache    : busca as MESMAS cartas de novo em paralelo → espera-se
                      X-Cache: HIT (somente Redis, sem tocar na API externa)

Ao final imprime tempo total, latência (min/p50/média/p95/max), distribuição de
HIT/MISS e o speedup da fase de cache em relação à fase de populate.

Executar:
    ./mvnw quarkus:dev
    pip install requests
    python stress_test.py
    python stress_test.py --concorrencia 8 --repeticoes 3 --cartas 15
"""

from __future__ import annotations

import argparse
import math
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field

import requests

MTG_API_CARDS = "https://api.magicthegathering.io/v1/cards"
TIMEOUT_SEGUNDOS = 30


# ---------------------------------------------------------------------------
# Modelo
# ---------------------------------------------------------------------------
@dataclass
class Resultado:
    carta_id: str
    status: int | None = None
    x_cache: str | None = None
    fonte: str | None = None
    latencia_ms: float = 0.0
    erro: str | None = None

    @property
    def ok(self) -> bool:
        return self.erro is None and self.status == 200


@dataclass
class Fase:
    nome: str
    resultados: list[Resultado] = field(default_factory=list)
    duracao_s: float = 0.0

    @property
    def latencias(self) -> list[float]:
        return [r.latencia_ms for r in self.resultados]

    @property
    def hits(self) -> int:
        return sum(1 for r in self.resultados if r.x_cache == "HIT")

    @property
    def misses(self) -> int:
        return sum(1 for r in self.resultados if r.x_cache == "MISS")

    @property
    def sucessos(self) -> int:
        return sum(1 for r in self.resultados if r.ok)

    @property
    def falhas(self) -> int:
        return sum(1 for r in self.resultados if not r.ok)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def obter_ids_cartas(quantidade: int) -> list[str]:
    """Busca ids reais de cartas na API Magic: The Gathering para usar na carga."""
    resposta = requests.get(MTG_API_CARDS, params={"pageSize": quantidade}, timeout=TIMEOUT_SEGUNDOS)
    resposta.raise_for_status()
    cartas = resposta.json().get("cards", [])
    ids = [c["id"] for c in cartas if c.get("id")]
    if len(ids) < quantidade:
        raise SystemExit(f"A API MTG retornou apenas {len(ids)} cartas (pedido: {quantidade})")
    return ids[:quantidade]


def limpar_cache(base_url: str, ids: list[str]) -> list[str]:
    """Remove entradas pré-existentes para garantir que a fase 1 seja um MISS real.

    Retorna os ids cuja invalidação não pôde ser confirmada.
    """
    com_erro: list[str] = []
    for carta_id in ids:
        try:
            resposta = requests.delete(f"{base_url}/carta/{carta_id}/cache", timeout=TIMEOUT_SEGUNDOS)
            if resposta.status_code not in (200, 204, 404):
                com_erro.append(carta_id)
        except requests.RequestException:
            com_erro.append(carta_id)
    return com_erro


def buscar_carta(base_url: str, carta_id: str) -> Resultado:
    """Executa GET /carta/{id} medindo a latência da chamada."""
    inicio = time.perf_counter()
    try:
        resposta = requests.get(f"{base_url}/carta/{carta_id}", timeout=TIMEOUT_SEGUNDOS)
        latencia_ms = (time.perf_counter() - inicio) * 1000
        return Resultado(
            carta_id=carta_id,
            status=resposta.status_code,
            x_cache=resposta.headers.get("X-Cache"),
            fonte=resposta.json().get("fonte") if resposta.status_code == 200 else None,
            latencia_ms=latencia_ms,
        )
    except (requests.RequestException, ValueError) as exc:
        return Resultado(
            carta_id=carta_id,
            latencia_ms=(time.perf_counter() - inicio) * 1000,
            erro=f"{type(exc).__name__}: {exc}",
        )


def executar_fase(nome: str, base_url: str, ids: list[str], concorrencia: int) -> Fase:
    """Dispara as consultas em paralelo e agrega os resultados."""
    fase = Fase(nome=nome)
    inicio = time.perf_counter()

    with ThreadPoolExecutor(max_workers=concorrencia) as pool:
        futuros = {pool.submit(buscar_carta, base_url, carta_id): carta_id for carta_id in ids}
        for futuro in as_completed(futuros):
            fase.resultados.append(futuro.result())

    fase.duracao_s = time.perf_counter() - inicio
    return fase


# ---------------------------------------------------------------------------
# Relatório
# ---------------------------------------------------------------------------
def _percentil(valores: list[float], pct: float) -> float:
    if not valores:
        return 0.0
    ordenado = sorted(valores)
    if len(ordenado) == 1:
        return ordenado[0]
    pos = (len(ordenado) - 1) * (pct / 100.0)
    base = math.floor(pos)
    resto = pos - base
    if base + 1 >= len(ordenado):
        return ordenado[base]
    return ordenado[base] + resto * (ordenado[base + 1] - ordenado[base])


def imprimir_fase(fase: Fase) -> None:
    latencias = fase.latencias
    print(f"\n  {fase.nome}")
    print(f"    {'requisições':<20}: {len(fase.resultados)} "
          f"({fase.sucessos}x 200, {fase.falhas} falha(s))")
    print(f"    {'X-Cache':<20}: {fase.hits} HIT | {fase.misses} MISS | "
          f"{len(fase.resultados) - fase.hits - fase.misses} sem header")
    print(f"    {'tempo total':<20}: {fase.duracao_s:.3f} s")
    if latencias:
        print(
            f"    {'latência (ms)':<20}: min {_percentil(latencias, 0):.1f} | "
            f"p50 {_percentil(latencias, 50):.1f} | "
            f"média {sum(latencias) / len(latencias):.1f} | "
            f"p95 {_percentil(latencias, 95):.1f} | "
            f"max {_percentil(latencias, 100):.1f}"
        )
    for falha in (r for r in fase.resultados if not r.ok):
        print(f"    ! carta {falha.carta_id}: status={falha.status} erro={falha.erro}")


def media(latencias: list[float]) -> float:
    return sum(latencias) / len(latencias) if latencias else 0.0


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Teste de estresse do cache-aside de cartas")
    parser.add_argument("--base-url", default="http://localhost:8080", help="URL base da API Quarkus")
    parser.add_argument("--cartas", type=int, default=15, help="Quantidade de cartas (padrão: 15)")
    parser.add_argument("--concorrencia", type=int, default=8, help="Threads em paralelo (padrão: 8)")
    parser.add_argument("--repeticoes", type=int, default=1, help="Rodadas da fase de cache (padrão: 1)")
    parser.add_argument("--sem-limpar-cache", action="store_true",
                        help="Não invalida o cache antes de começar")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    base_url = args.base_url.rstrip("/")

    print("=" * 74)
    print(" Teste de estresse — cache-aside de cartas")
    print("=" * 74)
    print(f" API          : {base_url}")
    print(f" Cartas       : {args.cartas}")
    print(f" Concorrência : {args.concorrencia} threads")
    print(f" Rodadas cache: {args.repeticoes}")

    print("\n[1/4] Buscando ids de cartas na API Magic: The Gathering...")
    ids = obter_ids_cartas(args.cartas)
    print(f"        {len(ids)} cartas carregadas")

    if not args.sem_limpar_cache:
        print("[2/4] Limpando cache existente (DELETE /carta/{id}/cache)...")
        erros_limpeza = limpar_cache(base_url, ids)
        if erros_limpeza:
            print(f"        ! {len(erros_limpeza)} invalidação(ões) não confirmada(s)")
    else:
        print("[2/4] Cache preservado (--sem-limpar-cache)")

    print(f"[3/4] FASE 1 — populate: {args.cartas} buscas paralelas (API + Redis)")
    fase1 = executar_fase("populate (miss esperado)", base_url, ids, args.concorrencia)

    print(f"[4/4] FASE 2 — cache: {args.repeticoes} rodada(s) de {args.cartas} buscas paralelas")
    fases_cache = [
        executar_fase(f"cache — rodada {n + 1} (hit esperado)", base_url, ids, args.concorrencia)
        for n in range(args.repeticoes)
    ]

    print("\n" + "-" * 74)
    print(" RESULTADOS")
    print("-" * 74)
    imprimir_fase(fase1)
    for fase_cache in fases_cache:
        imprimir_fase(fase_cache)

    # ------------------------------------------------------------------
    # Análise
    # ------------------------------------------------------------------
    lat1 = fase1.latencias
    lat_cache = [lat for fase in fases_cache for lat in fase.latencias]
    hits_cache = sum(fase.hits for fase in fases_cache)
    total_cache = sum(len(fase.resultados) for fase in fases_cache)
    falhas_http = fase1.falhas + sum(fase.falhas for fase in fases_cache)

    # Um MISS na fase de cache só é esperado se a carta falhou na fase 1
    # (logo, nunca entrou no Redis).
    ids_com_falha_fase1 = {r.carta_id for r in fase1.resultados if not r.ok}
    misses_inesperados = [
        r for fase in fases_cache for r in fase.resultados
        if r.x_cache != "HIT" and r.carta_id not in ids_com_falha_fase1
    ]

    print("\n" + "-" * 74)
    print(" ANÁLISE")
    print("-" * 74)
    if lat1 and lat_cache:
        speedup = media(lat1) / media(lat_cache)
        print(f"  {'latência média fase 1':<28}: {media(lat1):.1f} ms")
        print(f"  {'latência média fase 2':<28}: {media(lat_cache):.1f} ms")
        print(f"  {'speedup (fase 1 / 2)':<28}: {speedup:.2f}x")
        print(f"  {'redução de latência':<28}: {(1 - media(lat_cache) / media(lat1)) * 100:.1f}%")
    print(f"  {'HITs na fase de cache':<28}: {hits_cache}/{total_cache}")
    print(f"  {'misses inesperados':<28}: {len(misses_inesperados)}")
    print(f"  {'falhas HTTP':<28}: {falhas_http}")
    if ids_com_falha_fase1:
        print(f"  {'cartas com falha na fase 1':<28}: {len(ids_com_falha_fase1)} (não popularam o cache)")

    sucesso = falhas_http == 0 and not misses_inesperados and hits_cache == total_cache
    print("\n" + "=" * 74)
    if sucesso:
        print(f" OK — {fase1.misses} miss(es) popularam o cache e as "
              f"{total_cache} consultas seguintes vieram do Redis (HIT)")
        print("=" * 74)
        return 0

    print(" FALHA — veja os diagnósticos abaixo")
    if falhas_http:
        print(f"  • {falhas_http} resposta(s) fora de 200: a API Quarkus respondeu erro ou a requisição expirou")
        print("    → reduza --concorrencia ou aumente quarkus.rest-client.mtgApiUrl.read-timeout")
    if misses_inesperados:
        ids = ", ".join(r.carta_id for r in misses_inesperados)
        print(f"  • {len(misses_inesperados)} consulta(s) não vieram do cache na fase 2: {ids}")
    if fase1.misses == 0:
        print("  • nenhum MISS na fase 1: cache já estava populado "
              "(rode sem --sem-limpar-cache para limpar antes)")
    if fase1.falhas:
        print(f"  • {fase1.falhas} carta(s) falharam no populate e não entraram no Redis")
    print("=" * 74)
    return 1


if __name__ == "__main__":
    sys.exit(main())
