"""Gera docs/img/diagrama-arquitetura.png (arquitetura + fluxo CI/CD numerado). Uso: python gerar_diagrama.py"""
from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1920, 1260
img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)
F = lambda s, b=False: ImageFont.truetype("C:/Windows/Fonts/" + ("arialbd.ttf" if b else "arial.ttf"), s)
BLUE, ORANGE, GREEN, PURPLE, GRAY, RED = "#1f5fbf", "#d9680f", "#1f8a4c", "#6b3fa0", "#555555", "#b3261e"

def box(x1, y1, x2, y2, fill, line, title, lines=()):
    d.rounded_rectangle((x1, y1, x2, y2), radius=18, fill=fill, outline=line, width=3)
    d.text(((x1 + x2) // 2, y1 + 18), title, font=F(25, True), fill=line, anchor="mt")
    y = y1 + 60
    for t in lines:
        d.text(((x1 + x2) // 2, y), t, font=F(20), fill="#222222", anchor="mt")
        y += 29

def arrow(points, color, dash=False):
    for a, b in zip(points, points[1:]):
        d.line((a, b), fill=color, width=4)
    (x0, y0), (x1, y1) = points[-2], points[-1]
    dx = (x1 > x0) - (x1 < x0); dy = (y1 > y0) - (y1 < y0); s = 13
    if dx: d.polygon([(x1, y1), (x1 - dx * s * 1.6, y1 - s), (x1 - dx * s * 1.6, y1 + s)], fill=color)
    else:  d.polygon([(x1, y1), (x1 - s, y1 - dy * s * 1.6), (x1 + s, y1 - dy * s * 1.6)], fill=color)

def badge(x, y, n, color):
    d.ellipse((x - 19, y - 19, x + 19, y + 19), fill=color, outline="white", width=2)
    d.text((x, y), str(n), font=F(22, True), fill="white", anchor="mm")

def label(x, y, t, color):
    d.text((x, y), t, font=F(19), fill=color, anchor="mm")

d.text((W // 2, 18), "PetFlow · Arquitetura e fluxo CI/CD (Azure DevOps + Azure)", font=F(32, True), fill="#222", anchor="mt")

# ---- Linha 1: persona + Azure DevOps
y1, y2 = 90, 260
box(30, y1, 250, y2, "#f2f2f2", GRAY, "Desenvolvedor", ["VS Code (WSL)", "Git"])
box(330, y1, 580, y2, "#f3ecfa", PURPLE, "Azure Boards", ["Task (Scrum)", "cria a branch"])
box(660, y1, 930, y2, "#f3ecfa", PURPLE, "Azure Repos (Git)", ["branch master", "PR + merge"])
box(1010, y1, 1380, y2, "#eaf1fc", BLUE, "Pipeline CI (YAML)", ["trigger: master", "4  mvn verify · testes JUnit", "5  publica artefato JAR"])
box(1460, y1, 1890, y2, "#eaf1fc", BLUE, "Pipeline CD (YAML)", ["6  dispara com novo artefato", "8  deploy · 9  smoke test"])
arrow([(250, 175), (330, 175)], GRAY);   badge(290, 140, 1, GRAY)
arrow([(580, 175), (660, 175)], PURPLE); badge(620, 140, 2, PURPLE)
arrow([(930, 175), (1010, 175)], BLUE);  badge(970, 140, 3, BLUE)
arrow([(1380, 175), (1460, 175)], BLUE); badge(1420, 140, 5, BLUE)

# ---- Variable Group
box(1650, 330, 1890, 440, "#fff8e5", RED, "Variable Group", ["segredos (cadeado)"])
arrow([(1770, 330), (1770, 260)], RED); badge(1810, 295, 7, RED)
label(1700, 295, "segredos", RED)

# ---- Espelho GitHub
box(30, 330, 460, 430, "#efefef", GRAY, "GitHub (espelho)", ["sincronizado a cada merge"])
arrow([(795, 260), (795, 295), (245, 295), (245, 330)], GRAY); badge(520, 295, 12, GRAY)

# ---- Azure
d.rounded_rectangle((30, 500, 1890, 1090), radius=24, fill="#fafbfd", outline="#7a8aa0", width=3)
d.text((55, 515), "Microsoft Azure · 561413-petflow-s4-devops-rg", font=F(23, True), fill="#44546a", anchor="la")
box(120, 590, 640, 900, "#fdf1e7", ORANGE, "Scripts Azure CLI (scripts/)", ["01-create-infra.sh", "deploy-app.sh · smoke-test.sh", "sql.sh · 99-delete-infra.sh", ".env.azure fora do Git"])
box(760, 590, 1230, 900, "#fff4ea", ORANGE, "Azure Web App", ["Linux · plano B1 · Java 21", "Spring Boot 4 · JWT", "Swagger /swagger", "/actuator/health", "App Settings = segredos"])
box(1350, 590, 1820, 900, "#eaf7f0", GREEN, "Azure Database for PostgreSQL", ["Flexible Server 16 · PaaS", "Burstable B1ms · 32 GB", "disco gerenciado e persistente", "SSL obrigatório · backup", "Flyway cria o schema"])
arrow([(640, 745), (760, 745)], ORANGE); label(700, 720, "cria", ORANGE)
arrow([(1230, 700), (1350, 700)], GREEN); arrow([(1350, 790), (1230, 790)], GREEN)
label(1290, 745, "JDBC + SSL", GREEN); badge(1290, 655, 11, GREEN)

# CD -> Web App (cotovelo)
arrow([(1520, 260), (1520, 470), (1100, 470), (1100, 590)], BLUE)
badge(1520, 380, 8, BLUE)
label(1310, 448, "az webapp deploy do JAR + health check", BLUE)

d.text((60, 950), "9  Smoke test do CD: CRUD de Pets + agendamento de evento na URL pública", font=F(21, True), fill=BLUE, anchor="la")
d.text((60, 990), "Segredos: Variable Group → variáveis de ambiente → App Settings", font=F(19), fill="#444", anchor="la")
d.text((60, 1020), "(nunca no código nem nos YAMLs)", font=F(19), fill="#444", anchor="la")

# ---- Usuario final
box(790, 1140, 1210, 1235, "#f2f2f2", GRAY, "Usuário final", ["navegador / Swagger"])
arrow([(1000, 1140), (1000, 900)], GRAY); badge(1040, 1040, 10, GRAY); label(1115, 1040, "HTTPS", GRAY)
d.text((1500, 1190), "Números = ordem do fluxo (veja docs/architecture.md)", font=F(19), fill="#444", anchor="mm")

os.makedirs("../docs/img", exist_ok=True)
img.save("../docs/img/diagrama-arquitetura.png")
print("ok", img.size)
