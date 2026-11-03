"""Gera o PDF de entrega da Sprint 4 (DevOps). O enunciado exige que o PDF contenha SOMENTE:
nome completo, RM, turma de cada integrante, link do GitHub, link do vídeo e link do projeto Azure DevOps.
Preencha os LINK_* abaixo (depois de gravar o vídeo) e rode:  python gerar_pdf.py
"""
import fitz

SAIDA = "PetFlow_DevOps_Sprint4.pdf"
LINK_GITHUB = "https://github.com/lgaxd/petflow-devops-sprint4"
LINK_VIDEO = "https://www.youtube.com/watch?v=bukwmfzxSWI"
LINK_AZURE_DEVOPS = "https://dev.azure.com/RM561469/PetFlow"

INTEGRANTES = [
    ("Lucas Grillo Alcântara", "561413", "2TDSPX"),
    ("Pietro Ferreira Gomes Abrahamian", "561469", "2TDSPX"),
    ("Pedro Peres Benitez", "561792", "2TDSPX"),
    ("Lucca Ramos Mussumecci", "562027", "2TDSPX"),
]

CSS = """
body { font-family: sans-serif; font-size: 12pt; color: #111; }
h1 { font-size: 16pt; margin-top: 22pt; margin-bottom: 8pt; }
p { margin-top: 0; margin-bottom: 8pt; }
table { border-collapse: collapse; width: 100%; }
td, th { border: 1px solid #888; padding: 6pt; text-align: left; }
th { background-color: #eeeeee; }
.c { text-align: center; }
"""

linhas = "".join(f"<tr><td>{n}</td><td>{rm}</td><td>{t}</td></tr>" for n, rm, t in INTEGRANTES)
html = f"""
<p class="c" style="margin-top:30pt"><img src="logo-fiap.jpeg" width="200"/></p>
<p class="c"><b>FIAP · DevOps Tools &amp; Cloud Computing · Sprint 4</b></p>
<p class="c"><b>PetFlow · CI/CD no Azure DevOps</b></p>
<h1>Integrantes</h1>
<table>
<tr><th>Nome completo</th><th>RM</th><th>Turma</th></tr>
{linhas}
</table>
<h1>Links</h1>
<p><b>GitHub:</b> {LINK_GITHUB}</p>
<p><b>Vídeo no YouTube:</b> {LINK_VIDEO}</p>
<p><b>Projeto Azure DevOps:</b> {LINK_AZURE_DEVOPS}</p>
"""

story = fitz.Story(html, user_css=CSS, archive=fitz.Archive("assets"))
writer = fitz.DocumentWriter(SAIDA)
mediabox = fitz.paper_rect("a4")
where = mediabox + (60, 56, -60, -56)
more = 1
while more:
    dev = writer.begin_page(mediabox)
    more, _ = story.place(where)
    story.draw(dev)
    writer.end_page()
writer.close()

doc = fitz.open(SAIDA)
for page in doc:
    for url in (LINK_GITHUB, LINK_VIDEO, LINK_AZURE_DEVOPS):
        if url.startswith("http"):
            for r in page.search_for(url):
                page.insert_link({"kind": fitz.LINK_URI, "from": r, "uri": url})
doc.set_metadata({"title": "PetFlow · DevOps Sprint 4", "author": "; ".join(n for n, _, _ in INTEGRANTES)})
doc.saveIncr()
print("páginas:", len(doc))
