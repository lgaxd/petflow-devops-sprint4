"""Teste de fumaça ponta a ponta contra uma API em execução (banco real).

Uso:  python scripts/e2e_smoke.py [http://localhost:8080]

Lê TEST_TUTOR_EMAIL/TEST_TUTOR_PASSWORD (e, opcionalmente, ADMIN_EMAIL/ADMIN_PASSWORD)
do arquivo .env. Cria/usa a conta fixa e2e-fixed@petflow.test e remove pet, evento e
assinatura criados. Sem credenciais de ADMIN, os endpoints exclusivos de ADMIN são pulados.
"""
import json, os, sys, time, urllib.request, urllib.error

BASE = sys.argv[1] if len(sys.argv) > 1 else os.environ.get('E2E_BASE_URL', 'http://localhost:8080')
env = {}
with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '.env'), encoding='utf-8') as env_file:
    for line in env_file:
        if '=' in line and not line.startswith('#'):
            k, v = line.strip().split('=', 1)
            env[k] = v

results = []

def call(method, path, token=None, body=None, expect=None, label=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if token: req.add_header('Authorization', 'Bearer ' + token)
    if data is not None: req.add_header('Content-Type', 'application/json')
    try:
        with urllib.request.urlopen(req) as r:
            code, text = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        code, text = e.code, e.read().decode()
    try: js = json.loads(text) if text else None
    except ValueError: js = text
    ok = expect is None or code == expect
    results.append(ok)
    print(('PASS' if ok else 'FAIL'), method, path, '->', code, (label or ''), '' if ok else ('expected %s body=%s' % (expect, str(js)[:200])))
    return code, js

def login(email, pw):
    c, js = call('POST', '/auth/login', body={'email': email, 'password': pw}, expect=200, label='login ' + email)
    return js['token'], js

# --- auth
call('POST', '/auth/login', body={'email': env['TEST_TUTOR_EMAIL'], 'password': 'wrong-password1'}, expect=401, label='bad password')
call('GET', '/pets', expect=401, label='anonymous')
mtoken, mjs = login(env['TEST_TUTOR_EMAIL'], env['TEST_TUTOR_PASSWORD'])
print('seed user role:', mjs['role'])

# --- e2e tutor (fixed account so repeated runs leave no extra rows)
stamp = str(int(time.time()))
email = 'e2e-fixed@petflow.test'
c, _ = call('POST', '/auth/login', body={'email': email, 'password': 'senha12345'})
if c != 200:
    c, tutor = call('POST', '/tutors', body={'name': 'E2E Tutor', 'email': email, 'phone': '11999990000', 'password': 'senha12345'}, expect=201)
call('POST', '/tutors', body={'name': 'E2E Tutor', 'email': email, 'phone': '1', 'password': 'senha12345'}, expect=409, label='duplicate email')
call('POST', '/tutors', body={'name': '', 'email': 'x', 'password': '1'}, expect=400, label='validation')
ttoken, tjs = login(email, 'senha12345')
tid = tjs['id']

# --- renovacao de token e tabelas de dominio
assert tjs.get('expiresAt'), 'login sem expiresAt'
call('POST', '/auth/refresh', expect=401, label='refresh sem token')
call('POST', '/auth/refresh', token='abc.def.ghi', expect=401, label='refresh token adulterado')
c, renewed = call('POST', '/auth/refresh', token=ttoken, expect=200, label='refresh')
assert renewed['id'] == tid and renewed['token'], 'refresh devolveu outro usuario'
ttoken = renewed['token']
call('GET', '/species', expect=401, label='species sem token')
c, species = call('GET', '/species', token=ttoken, expect=200, label='species')
c, event_types = call('GET', '/event-types', token=ttoken, expect=200, label='event-types')
assert species and event_types, 'tabelas de dominio vazias'

# --- admin only checks
call('GET', '/tutors', token=ttoken, expect=403, label='tutor lists tutors')
admin = False
atoken = None
if env.get('ADMIN_EMAIL') and env.get('ADMIN_PASSWORD'):
    atoken, ajs = login(env['ADMIN_EMAIL'], env['ADMIN_PASSWORD'])
    admin = ajs['role'] == 'ADMIN'
else:
    print('SKIP admin-only endpoints (set ADMIN_EMAIL/ADMIN_PASSWORD in .env to run them)')
if admin:
    call('GET', '/tutors?size=2&sortBy=name', token=atoken, expect=200)
    call('GET', '/tutors/%d' % tid, token=atoken, expect=200)
    call('PUT', '/tutors/%d' % tid, token=atoken, body={'name': 'E2E Renamed', 'email': email, 'phone': '11999990001', 'password': ''}, expect=200)
    call('GET', '/tutors?name=E2E', token=atoken, expect=200, label='filter by name')
    call('GET', '/tutors?sortBy=password', token=atoken, expect=400, label='invalid sort')

# --- pets
c, pet = call('POST', '/pets', token=ttoken, body={'name': 'Rex', 'breed': 'SRD', 'birthDate': '2015-05-20', 'weight': 32.5, 'tutorId': tid, 'speciesId': 1}, expect=201)
pid = pet['id']
call('POST', '/pets', token=ttoken, body={'name': 'Hack', 'tutorId': 1, 'speciesId': 1}, expect=403, label='pet for another tutor')
call('GET', '/pets', token=ttoken, expect=200)
call('GET', '/pets?name=re', token=ttoken, expect=200, label='filter name')
call('GET', '/pets/%d' % pid, token=ttoken, expect=200)
call('PUT', '/pets/%d' % pid, token=ttoken, body={'name': 'Rex II', 'breed': 'SRD', 'birthDate': '2015-05-20', 'weight': 33.0, 'tutorId': tid, 'speciesId': 1}, expect=200)
call('GET', '/pets/999999999', token=ttoken, expect=404)

# --- gamification
c, pts = call('GET', '/gamification/points', token=ttoken, expect=200)
print('  points after pet:', pts['totalPoints'])
call('GET', '/gamification/pets/%d/risk' % pid, token=ttoken, expect=200)
call('GET', '/gamification/coupons/available', token=ttoken, expect=200)

# --- health events
c, ev = call('POST', '/health-events', token=ttoken, body={'description': 'Vacina', 'eventDate': '2026-10-01', 'status': 'AGENDADO', 'petId': pid, 'eventTypeId': 1}, expect=201)
eid = ev['id']
call('GET', '/health-events?petId=%d&status=AGENDADO' % pid, token=ttoken, expect=200)
call('GET', '/health-events/%d' % eid, token=ttoken, expect=200)
call('PUT', '/health-events/%d' % eid, token=ttoken, body={'description': 'Vacina ok', 'eventDate': '2026-10-01', 'status': 'REALIZADO', 'petId': pid, 'eventTypeId': 1}, expect=200, label='-> REALIZADO')
call('PUT', '/health-events/%d' % eid, token=ttoken, body={'description': 'Vacina ok', 'eventDate': '2026-10-01', 'status': 'AGENDADO', 'petId': pid, 'eventTypeId': 1}, expect=422, label='invalid transition')

# --- agendamento de datas futuras: AGENDADO/CANCELADO aceitam; REALIZADO nao
future = time.strftime('%Y-%m-%d', time.localtime(time.time() + 30 * 86400))
fut = {'description': 'Retorno', 'eventDate': future, 'status': 'AGENDADO', 'petId': pid, 'eventTypeId': 1}
c, fev = call('POST', '/health-events', token=ttoken, body=fut, expect=201, label='agendado com data futura')
c, bad = call('POST', '/health-events', token=ttoken, body=dict(fut, status='REALIZADO'), expect=422, label='realizado com data futura')
assert isinstance(bad, dict) and bad.get('code') == 'FUTURE_DATE_NOT_ALLOWED', 'codigo de erro inesperado: %s' % bad
call('PUT', '/health-events/%d' % fev['id'], token=ttoken, body=dict(fut, status='REALIZADO'), expect=422, label='concluir agendado com data futura')
call('PUT', '/health-events/%d' % fev['id'], token=ttoken, body=dict(fut, status='CANCELADO'), expect=200, label='cancelado mantem data futura')
call('DELETE', '/health-events/%d' % fev['id'], token=ttoken, expect=204, label='remove evento futuro')
c, pts2 = call('GET', '/gamification/points', token=ttoken, expect=200)
print('  points after event:', pts2['totalPoints'])
assert pts2['totalPoints'] > pts['totalPoints'], 'points not granted for realized event'

# --- admin catalog (clinic, plan, coupon)
cid = plid = None
if admin:
    c, clinic = call('POST', '/clinics', token=atoken, body={'name': 'E2E Clinic ' + stamp, 'address': 'Rua 1', 'phone': '1133334444', 'cnpj': '%s.%s.%s/0001-%s' % (stamp[-2:].rjust(2, '0'), stamp[-5:-2], stamp[-8:-5], stamp[-10:-8])}, expect=201)
    cid = clinic and clinic.get('id')
    call('POST', '/clinics', token=ttoken, body={'name': 'x', 'cnpj': '00.000.000/0001-00'}, expect=403, label='tutor creates clinic')
    if cid:
        call('GET', '/clinics?name=E2E', token=ttoken, expect=200)
        call('GET', '/clinics/%d' % cid, token=ttoken, expect=200)
        call('PUT', '/clinics/%d' % cid, token=atoken, body={'name': 'E2E Clinic 2', 'address': 'Rua 2', 'phone': '1133334444', 'cnpj': clinic['cnpj']}, expect=200)
        c, plan = call('POST', '/plans', token=atoken, body={'name': 'E2E Plan', 'description': 'd', 'price': 10.5, 'durationDays': 30, 'pointsPerEvent': 3, 'clinicId': cid}, expect=201)
        plid = plan['id']
        call('GET', '/plans?clinicId=%d' % cid, token=ttoken, expect=200)
        call('GET', '/plans/%d' % plid, token=ttoken, expect=200)
        call('PUT', '/plans/%d' % plid, token=atoken, body={'name': 'E2E Plan 2', 'description': 'd', 'price': 11.5, 'durationDays': 30, 'pointsPerEvent': 3, 'clinicId': cid}, expect=200)

sid = None
if not plid:
    c, plans = call('GET', '/plans?size=1', token=ttoken, expect=200, label='use seeded plan')
    if plans and plans.get('content'):
        plid = plans['content'][0]['id']
if plid:
    # status omitted on purpose (must default to ATIVO)
    c, sub = call('POST', '/subscriptions', token=ttoken, body={'startDate': '2026-10-01', 'petId': pid, 'planId': plid}, expect=201, label='status omitted')
    sid = sub['id']
    print('  subscription status/endDate:', sub['status'], sub['endDate'])
    call('POST', '/subscriptions', token=ttoken, body={'startDate': '2026-10-10', 'endDate': '2026-10-01', 'petId': pid, 'planId': plid}, expect=422, label='end before start')
    call('GET', '/subscriptions?petId=%d' % pid, token=ttoken, expect=200)
    call('GET', '/subscriptions/%d' % sid, token=ttoken, expect=200)
    call('PUT', '/subscriptions/%d/status?status=CANCELADO' % sid, token=ttoken, expect=200)
    call('PUT', '/subscriptions/%d/status?status=ATIVO' % sid, token=ttoken, expect=422, label='reactivate')

# --- coupons / redeem
c, cat = call('GET', '/gamification/coupons/available?size=1', token=ttoken, expect=200)
if cat and cat.get('content'):
    cp = cat['content'][0]
    call('POST', '/gamification/redeem', token=ttoken, body={'couponId': cp['id']}, expect=422 if cp['pointsRequired'] > pts2['totalPoints'] + 20 else None, label='redeem (points %s)' % cp['pointsRequired'])
call('GET', '/coupons?status=DISPONIVEL', token=ttoken, expect=200)
call('GET', '/coupons?status=BAD', token=ttoken, expect=400)
call('GET', '/redeems', token=ttoken, expect=200)
if admin:
    call('GET', '/redeems', token=atoken, expect=200, label='admin sees all')

call('GET', '/rota-inexistente', token=ttoken, expect=404)
call('GET', '/actuator/health', expect=200)
call('GET', '/swagger', expect=None)
call('GET', '/api-docs', expect=200)

# --- cleanup
if sid: call('DELETE', '/subscriptions/%d' % sid, token=ttoken, expect=204)
call('DELETE', '/health-events/%d' % eid, token=ttoken, expect=None, label='delete event (may 409 if points reference it)')
call('DELETE', '/pets/%d' % pid, token=ttoken, expect=None, label='delete pet')
if admin:
    if plid and cid: call('DELETE', '/plans/%d' % plid, token=atoken, expect=204)
    if cid: call('DELETE', '/clinics/%d' % cid, token=atoken, expect=204)

print('\n%d/%d expectations passed' % (sum(results), len(results)))
sys.exit(0 if all(results) else 1)
