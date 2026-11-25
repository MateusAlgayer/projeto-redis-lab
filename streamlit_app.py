"""
Streamlit UI para gerenciamento de Promoções via API REST (Quarkus + Redis).

Consome os endpoints definidos em PromocaoResource.java:
  POST   /promocao                          → Adicionar promoção
  GET    /promocao/{nome}                   → Consultar promoção
  GET    /promocao/existe/{nome}            → Verificar existência
  GET    /promocao/tempoRestante/{nome}     → Tempo restante (TTL)
  GET    /promocao/listar                   → Listar todas
  PUT    /promocao/prorrogar/{nome}         → Prorrogar validade

Executar:
    pip install streamlit requests
    streamlit run streamlit_app.py
"""

import streamlit as st
import requests
from datetime import datetime, timedelta

# ---------------------------------------------------------------------------
# Configuração
# ---------------------------------------------------------------------------
st.set_page_config(page_title="Promoções – Redis Lab", page_icon="🏷️", layout="wide")

API_BASE = st.sidebar.text_input("🔗 URL base da API", value="http://localhost:8080")
API_URL = f"{API_BASE.rstrip('/')}/promocao"

st.title("🏷️ Gerenciamento de Promoções")
st.caption("Interface Streamlit para a API REST de promoções com Redis")

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _fmt_dt(iso: str | None) -> str:
    """Formata um datetime ISO para exibição amigável."""
    if not iso:
        return "—"
    try:
        dt = datetime.fromisoformat(iso)
        return dt.strftime("%d/%m/%Y %H:%M:%S")
    except (ValueError, TypeError):
        return str(iso)


def _fmt_ttl(seconds: int | None) -> str:
    """Converte TTL em segundos para formato legível."""
    if seconds is None or seconds < 0:
        return "Expirada / não encontrada"
    delta = timedelta(seconds=seconds)
    parts = []
    days = delta.days
    hours, remainder = divmod(delta.seconds, 3600)
    minutes, secs = divmod(remainder, 60)
    if days:
        parts.append(f"{days}d")
    if hours:
        parts.append(f"{hours}h")
    if minutes:
        parts.append(f"{minutes}min")
    parts.append(f"{secs}s")
    return " ".join(parts)


# ---------------------------------------------------------------------------
# Abas
# ---------------------------------------------------------------------------
tab_listar, tab_consultar, tab_criar, tab_prorrogar = st.tabs([
    "📋 Listar Promoções",
    "🔍 Consultar / Verificar",
    "➕ Nova Promoção",
    "⏳ Prorrogar Promoção",
])

# ---- Listar Promoções -----------------------------------------------------
with tab_listar:
    st.subheader("Promoções ativas")
    st.caption("🔄 Atualização automática a cada 1 segundos")

    @st.fragment(run_every=1)
    def _painel_promocoes():
        try:
            resp = requests.get(f"{API_URL}/listar", timeout=5)
            if resp.status_code == 200:
                promocoes = resp.json()
                if not promocoes:
                    st.info("Nenhuma promoção ativa encontrada.")
                else:
                    for promo in promocoes:
                        with st.container(border=True):
                            cols = st.columns([2, 1, 1, 1])
                            cols[0].markdown(f"**{promo.get('nome', '—')}**")
                            cols[1].metric("Desconto", f"{promo.get('percentualDesconto', 0):.1f}%")
                            cols[2].write(f"📅 Expira em: **{_fmt_dt(promo.get('dataExpiracao'))}**")

                            # Buscar TTL em tempo real
                            nome = promo.get("nome")
                            if nome:
                                try:
                                    ttl_resp = requests.get(f"{API_URL}/tempoRestante/{nome}", timeout=3)
                                    if ttl_resp.status_code == 200:
                                        cols[3].write(f"⏱️ TTL: **{_fmt_ttl(ttl_resp.json())}**")
                                    else:
                                        cols[3].write("⏱️ TTL: —")
                                except requests.RequestException:
                                    cols[3].write("⏱️ TTL: erro")
            else:
                st.error(f"Erro ao listar promoções (HTTP {resp.status_code})")
        except requests.ConnectionError:
            st.error("❌ Não foi possível conectar à API. Verifique se o servidor está rodando.")
        except requests.RequestException as e:
            st.error(f"❌ Erro de comunicação: {e}")

    _painel_promocoes()

# ---- Consultar / Verificar ------------------------------------------------
with tab_consultar:
    st.subheader("Consultar promoção por nome")
    nome_consulta = st.text_input("Nome da promoção", key="nome_consulta", placeholder="ex: blackfriday")

    col_a, col_b, col_c = st.columns(3)

    # Consultar detalhes
    with col_a:
        if st.button("🔍 Consultar detalhes", disabled=not nome_consulta):
            try:
                resp = requests.get(f"{API_URL}/{nome_consulta}", timeout=5)
                if resp.status_code == 200:
                    promo = resp.json()
                    st.success("Promoção encontrada!")
                    st.json(promo)
                elif resp.status_code == 404:
                    st.warning("Promoção não encontrada ou já expirada.")
                else:
                    st.error(f"Erro HTTP {resp.status_code}")
            except requests.RequestException as e:
                st.error(f"Erro: {e}")

    # Verificar existência
    with col_b:
        if st.button("✅ Verificar existência", disabled=not nome_consulta):
            try:
                resp = requests.get(f"{API_URL}/existe/{nome_consulta}", timeout=5)
                if resp.status_code == 200:
                    st.success(f"✅ A promoção **{nome_consulta}** existe e está ativa!")
                elif resp.status_code == 404:
                    st.warning(f"⚠️ A promoção **{nome_consulta}** não existe ou expirou.")
                else:
                    st.error(f"Erro HTTP {resp.status_code}")
            except requests.RequestException as e:
                st.error(f"Erro: {e}")

    # Tempo restante
    with col_c:
        if st.button("⏱️ Tempo restante", disabled=not nome_consulta):
            try:
                resp = requests.get(f"{API_URL}/tempoRestante/{nome_consulta}", timeout=5)
                if resp.status_code == 200:
                    ttl = resp.json()
                    st.info(f"⏱️ Tempo restante: **{_fmt_ttl(ttl)}** ({ttl} segundos)")
                elif resp.status_code == 404:
                    st.warning("Promoção não encontrada ou expirada.")
                else:
                    st.error(f"Erro HTTP {resp.status_code}")
            except requests.RequestException as e:
                st.error(f"Erro: {e}")

# ---- Criar Promoção -------------------------------------------------------
with tab_criar:
    st.subheader("Adicionar nova promoção")

    with st.form("form_criar", clear_on_submit=True):
        nome_novo = st.text_input("Nome *", placeholder="ex: blackfriday")
        desconto = st.number_input(
            "Percentual de desconto (%)",
            min_value=0.0,
            max_value=100.0,
            value=10.0,
            step=0.5,
        )

        col_data, col_hora = st.columns(2)
        with col_data:
            data_exp = st.date_input("Data de expiração *", value=datetime.now().date() + timedelta(days=7))
        with col_hora:
            hora_exp = st.time_input("Hora de expiração *", value=datetime.now().time().replace(second=0, microsecond=0))

        submitted = st.form_submit_button("➕ Criar Promoção", type="primary")

        if submitted:
            if not nome_novo or not nome_novo.strip():
                st.error("O nome da promoção é obrigatório.")
            else:
                dt_expiracao = datetime.combine(data_exp, hora_exp)
                payload = {
                    "nome": nome_novo.strip(),
                    "percentualDesconto": desconto,
                    "dataExpiracao": dt_expiracao.strftime("%Y-%m-%dT%H:%M:%S"),
                }
                try:
                    resp = requests.post(API_URL, json=payload, timeout=5)
                    if resp.status_code == 201:
                        st.success(f"✅ Promoção **{nome_novo}** criada com sucesso!")
                        location = resp.headers.get("Location")
                        if location:
                            st.caption(f"Recurso criado em: `{location}`")
                    elif resp.status_code == 409:
                        st.error("Já existe uma promoção com esse nome.")
                    elif resp.status_code == 400:
                        st.error("Dados inválidos. Verifique os campos e tente novamente.")
                    else:
                        st.error(f"Erro HTTP {resp.status_code}")
                except requests.RequestException as e:
                    st.error(f"Erro: {e}")

# ---- Prorrogar Promoção ---------------------------------------------------
with tab_prorrogar:
    st.subheader("Prorrogar validade de uma promoção")

    nome_prorrogar = st.text_input("Nome da promoção *", key="nome_prorrogar", placeholder="ex: blackfriday")

    col_dp, col_hp = st.columns(2)
    with col_dp:
        nova_data = st.date_input(
            "Nova data de expiração *",
            value=datetime.now().date() + timedelta(days=30),
            key="prorr_data",
        )
    with col_hp:
        nova_hora = st.time_input(
            "Nova hora de expiração *",
            value=datetime.now().time().replace(second=0, microsecond=0),
            key="prorr_hora",
        )

    if st.button("⏳ Prorrogar", type="primary", disabled=not nome_prorrogar):
        nova_dt = datetime.combine(nova_data, nova_hora)
        payload = {
            "novaDataExpiracao": nova_dt.strftime("%Y-%m-%dT%H:%M:%S"),
        }
        try:
            resp = requests.put(f"{API_URL}/prorrogar/{nome_prorrogar}", json=payload, timeout=5)
            if resp.status_code == 200:
                st.success(f"✅ Promoção **{nome_prorrogar}** prorrogada com sucesso!")
                st.json(resp.json())
            elif resp.status_code == 400:
                st.error("Dados inválidos. Verifique a nova data de expiração.")
            elif resp.status_code == 404:
                st.warning("Promoção não encontrada ou já expirada.")
            else:
                st.error(f"Erro HTTP {resp.status_code}")
        except requests.RequestException as e:
            st.error(f"Erro: {e}")

# ---------------------------------------------------------------------------
# Rodapé
# ---------------------------------------------------------------------------
st.sidebar.markdown("---")
st.sidebar.markdown(
    "**Promoções Redis Lab**  \n"
    "Streamlit + Quarkus + Redis  \n"
    f"API: `{API_URL}`"
)
