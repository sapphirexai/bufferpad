import sys,json,subprocess,time,socket,urllib.request,urllib.error,http.cookiejar,threading,struct,traceback,uuid,concurrent.futures,argparse,hashlib
from pathlib import Path
root=next(p for p in Path(__file__).resolve().parents if (p/'wms-opc/pom.xml').exists())
sys.path.insert(0,str(root/'tmp/db-upgrade-libs'))
import pymysql
parser=argparse.ArgumentParser(description='Real TCP scanner/PLC acceptance, using a private loopback MySQL instance only.')
parser.add_argument('label',nargs='?',default='acceptance')
parser.add_argument('--jar',type=Path,default=root/'wms-opc/target/opc-0.0.1.-SNAPSHOT.jar')
parser.add_argument('--mysql-bin',type=Path,default=root/'bufferpad-installer/logs/runtime-smoke/mysql/bin')
parser.add_argument('--java',type=Path,default=root/'bufferpad-installer/logs/runtime-smoke/java/bin/java.exe')
args=parser.parse_args();label=args.label
assert label and all(c.isalnum() or c in '-_' for c in label),'Label must be a simple directory name'
run=root/'tmp'/('eight-operations-'+label);run.mkdir(exist_ok=True)
mysql_bin=args.mysql_bin;java=args.java;mysql_data=run/'mysql-data'
flags=subprocess.CREATE_NO_WINDOW
def port():
    with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
db_port,http_port=port(),port();base='http://127.0.0.1:'+str(http_port)
report={'status':'RUNNING','productionAccess':'NONE','jarSha256':hashlib.sha256(args.jar.read_bytes()).hexdigest(),'cases':[]}
def db(name='eight_operations_acceptance'):
    return pymysql.connect(host='127.0.0.1',port=db_port,user='root',database=name,charset='utf8mb4',autocommit=True,cursorclass=pymysql.cursors.DictCursor)
def sql(query,args=()):
    with db() as c:
        with c.cursor() as cur:cur.execute(query,args);return cur.fetchall()
def wait(fn,timeout=12):
    end=time.monotonic()+timeout;last=None
    while time.monotonic()<end:
        last=fn()
        if last:return last
        time.sleep(.05)
    raise AssertionError('Timed out waiting for '+str(fn)+' last='+str(last))
class Client:
    def __init__(self):self.cookies=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
    def call(self,method,path,payload=None,operation=None,expected=200):
        headers={}
        if method!='GET':headers['X-XSRF-TOKEN']=self.call('GET','/auth/csrf')['data']['token']
        if operation:headers['X-Operation-Id']=operation
        if payload is not None:headers['Content-Type']='application/json'
        req=urllib.request.Request(base+path,data=None if payload is None else json.dumps(payload).encode(),headers=headers,method=method)
        try:r=self.opener.open(req,timeout=30)
        except urllib.error.HTTPError as e:r=e
        with r:
            data=json.loads(r.read());assert r.status==expected,(path,r.status,data);return data
    def manual(self,qr,op=None):return self.call('POST','/cushion/manualCushionInfo',{'workLine':1,'qrCode':qr},op or str(uuid.uuid4()))
class Peer:
    def __init__(self,kind):
        self.kind=kind;self.packets=[];self.clients=[];self.mode='NORMAL';self.value=42;self.running=True
        self.socket=socket.socket();self.socket.bind(('127.0.0.1',0));self.socket.listen();self.port=self.socket.getsockname()[1]
        threading.Thread(target=self.accept,daemon=True).start()
    def accept(self):
        while self.running:
            try:c,_=self.socket.accept()
            except OSError:return
            self.clients.append(c);threading.Thread(target=self.serve,args=(c,),daemon=True).start()
    def serve(self,c):
        try:
            with c:
                if self.kind=='scanner':
                    while self.running:
                        b=c.recv(4096)
                        if not b:return
                        self.packets.append({'raw':b.hex()})
                    return
                f=c.makefile('rb')
                while self.running:
                    h=f.read(7)
                    if len(h)!=7:return
                    b=f.read(int.from_bytes(h[4:6],'big')-1)
                    fc=b[0];addr=int.from_bytes(b[1:3],'big')
                    record={'fc':fc,'addr':addr,'raw':b.hex(),'time':time.monotonic()}
                    if fc==16:record['value']=int.from_bytes(b[6:8],'big')
                    elif fc==6:record['value']=int.from_bytes(b[3:5],'big')
                    self.packets.append(record)
                    mode=self.mode
                    if mode=='SILENT' or mode=='READ_SILENT' and fc==3:continue
                    if mode=='CLOSE':return
                    if mode=='REJECT' or mode=='READ_REJECT' and fc==3:response=bytes([fc|128,2])
                    elif fc==3:response=bytes([3,2])+struct.pack('>h',self.value)
                    elif fc in (6,16):response=b[:5]
                    else:raise AssertionError('Unexpected Modbus function '+str(fc))
                    c.sendall(h[:4]+struct.pack('>H',len(response)+1)+h[6:7]+response)
        except (OSError,ValueError):pass
    def send(self,b):
        wait(lambda:self.clients);self.clients[-1].sendall(b)
    def close(self):
        self.running=False;self.socket.close()
        for c in self.clients:
            try:c.shutdown(socket.SHUT_RDWR);c.close()
            except OSError:pass
scanner=Peer('scanner');plc=Peer('plc');other=Peer('plc')
backend=None;mysql=None;logs=[];sse_response=None;sse_events=[]
def case(name,fn):
    try:fn();result={'name':name,'status':'PASS'};print('PASS '+name,flush=True)
    except Exception as e:
        result={'name':name,'status':'FAIL','error':str(e),'trace':traceback.format_exc()};print('FAIL '+name+' '+str(e),flush=True)
    report['cases'].append(result)
def seed(qr,used=1,maximum=500,recent=False,detail=True):
    sql('INSERT INTO cushion_info(qr_code,used_count,max_use_count,scanner_id,scanner_seq,work_line,scanner_position,last_scan_date) VALUES(%s,%s,%s,101,1,1,\'测试上\',IF(%s,NOW(),DATE_SUB(NOW(),INTERVAL 3 HOUR)))',(qr,used,maximum,recent))
    if detail:sql('INSERT INTO cushion_detail(qr_code,scanner_id,scanner_seq,work_line,scanner_position) VALUES(%s,101,1,1,\'测试上\')',(qr,))
def summaries(qr):return sql('SELECT * FROM scan_log WHERE qr_code=%s ORDER BY id',(qr,))
def finished(qr,count=1):
    def read():
        rows=summaries(qr)
        return rows if len(rows)>=count and all(r['status']!='PROCESSING' for r in rows) else None
    return wait(read,25)
def frame(qr,tpl=False):return ('[TPL_STX]'+qr+'[TPL_ETX]').encode() if tpl else b'\x02'+qr.encode()+b'\x03'
def biz_packets(start):return [p for p in plc.packets[start:] if p['addr']!=10005]
def scan_check(qr,write,read=None,manual=False,used=1,status='SUCCESS',result='SCAN_COUNTED',read_code=None,tpl=False):
    start=len(plc.packets)
    if manual:client.manual(qr)
    else:scanner.send(frame(qr,tpl))
    row=finished(qr)[-1];assert row['status']==status,row
    assert row['result_code']==result,row
    data=json.loads(row['detail_json'])
    if read_code:assert data['readCode']==read_code,data
    packets=biz_packets(start)
    expected=[(6,10000+write,1)]+([] if read is None else [(3,10000+read,None)])
    actual=[(p['fc'],p['addr'],p.get('value')) for p in packets]
    assert actual==expected,(actual,expected)
    cushion=sql('SELECT * FROM cushion_info WHERE qr_code=%s',(qr,))[0]
    assert cushion['used_count']==used,cushion
    assert len(summaries(qr))==1
    assert 'mock-scanner' in row['msg'] and 'mock-plc' in row['msg'] and '127.0.0.1' in row['msg'],row['msg']
    if read is not None and read_code in (None,'PLC_READ_SUCCEEDED'):
        assert cushion['open_count']==plc.value,cushion
        assert sql('SELECT open_count FROM cushion_detail WHERE qr_code=%s ORDER BY id DESC LIMIT 1',(qr,))[0]['open_count']==plc.value
    return row
try:
    if not mysql_data.exists():
        subprocess.run([str(mysql_bin/'mysqld.exe'),'--no-defaults','--initialize-insecure','--basedir='+str(mysql_bin.parent),'--datadir='+str(mysql_data)],stdout=(run/'initialize.log').open('wb'),stderr=subprocess.STDOUT,check=True,creationflags=flags)
    lf=(run/'mysql.log').open('wb');logs.append(lf)
    mysql=subprocess.Popen([str(mysql_bin/'mysqld.exe'),'--no-defaults','--basedir='+str(mysql_bin.parent),'--datadir='+str(mysql_data),'--port='+str(db_port),'--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin','--innodb-buffer-pool-size=134217728','--console'],stdout=lf,stderr=lf,creationflags=flags)
    for i in range(120):
        try:
            with db('mysql'):break
        except pymysql.OperationalError:time.sleep(.5)
    else:raise RuntimeError('MySQL unavailable')
    with db('mysql') as c:
        with c.cursor() as cur:
            cur.execute('DROP DATABASE IF EXISTS eight_operations_acceptance');cur.execute('CREATE DATABASE eight_operations_acceptance CHARACTER SET utf8mb4')
    for schema in [root/'bufferpad-installer/app/db/wms_opc.sql',root/'wms-opc/docs/sql/20260914_scan_log_operation_summary.sql',root/'wms-opc/docs/sql/20260914_scan_log_retention_index.sql']:
        subprocess.run([str(mysql_bin/'mysql.exe'),'--protocol=TCP','--host=127.0.0.1','--port='+str(db_port),'-uroot','--default-character-set=utf8mb4','eight_operations_acceptance'],input=schema.read_bytes(),capture_output=True,check=True,creationflags=flags)
    for id,typ,name,peer in [(101,0,'mock-scanner',scanner),(103,2,'mock-plc',plc),(104,2,'untargeted-plc',other)]:
        sql('INSERT INTO device_info(id,type,name,ip,port,work_line,install_seq,position,status) VALUES(%s,%s,%s,\'127.0.0.1\',%s,1,1,\'测试上\',1)',(id,typ,name,peer.port))
    sql('INSERT INTO opc_config(id,cushion_max_use_count) VALUES(1,500)')
    for typ in range(8):sql('INSERT INTO plc_addr(id,scanner_id,plc_id,type,addr) VALUES(%s,101,103,%s,%s)',(1000+typ,typ,'MW'+str(10000+typ)))
    lf=(run/'backend.log').open('wb');logs.append(lf)
    backend=subprocess.Popen([str(java),'-Xms128m','-Xmx384m','-Dfile.encoding=UTF-8','-jar',str(args.jar),'--spring.profiles.active=prod','--server.address=127.0.0.1','--server.port='+str(http_port),'--spring.datasource.druid.url=jdbc:mysql://127.0.0.1:'+str(db_port)+'/eight_operations_acceptance?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true','--spring.datasource.druid.username=root','--spring.datasource.druid.password=','--auth.allowed-origins='+base,'--scan-log.retention.enabled=false','--logging.file.name='+str(run/'application.log')],stdout=lf,stderr=lf,creationflags=flags,cwd=run)
    def healthy():
        try:
            with urllib.request.urlopen(base+'/actuator/health',timeout=1) as r:return r.status==200
        except Exception:return False
    wait(healthy,90);client=Client();client.call('POST','/auth/login',{'username':'superadmin','password':'example-maintenance-password-1'})
    sse_response=client.opener.open(base+'/sse/devicesStatus/1',timeout=120)
    def receive_sse():
        try:
            for line in sse_response:
                if line.startswith(b'data:'):sse_events.append(json.loads(line[5:].decode()))
        except (OSError,ValueError):pass
    threading.Thread(target=receive_sse,daemon=True).start()
    wait(lambda:scanner.clients and plc.clients and other.clients)
    case('02+06 automatic scan success then open count read',lambda:scan_check('AUTO-SUCCESS',2,6))
    case('04+07 manual scan success then open count read',lambda:scan_check('MANUAL-SUCCESS',4,7,manual=True))
    def no_read():
        before=sql('SELECT COUNT(*) n FROM cushion_info')[0]['n'];last=sql('SELECT COALESCE(MAX(id),0) n FROM scan_log')[0]['n'];start=len(plc.packets)
        scanner.send(b'\x02NoRead\x03')
        rows=wait(lambda:sql("SELECT * FROM scan_log WHERE id>%s AND status='FAILED'",(last,)))
        assert len(rows)==1 and rows[0]['result_code']=='SCAN_NO_READ',rows
        assert sql('SELECT COUNT(*) n FROM cushion_info')[0]['n']==before
        assert [(p['fc'],p['addr']) for p in biz_packets(start)]==[(6,10000)]
    case('00 NoRead writes failure once without cushion insertion',no_read)
    for manual in [False,True]:
        for recent in [False,True]:
            qr='MAX-'+str(manual)+'-'+str(recent);seed(qr,2 if recent else 1,2,recent)
            case(('03' if manual else '01')+' max boundary recent='+str(recent),lambda q=qr,m=manual:scan_check(q,3 if m else 1,manual=m,used=2,status='WARNING',result='CUSHION_MAX_REACHED'))
    def heartbeat():
        before=sql('SELECT COUNT(*) n FROM scan_log')[0]['n'];start=len(plc.packets);scanner.send(b'\x02HeartBeat\x03')
        wait(lambda:any(p['addr']==10005 for p in plc.packets[start:]),5)
        time.sleep(.2);assert sql('SELECT COUNT(*) n FROM scan_log')[0]['n']==before
        assert all(p['value']==0 for p in plc.packets[start:] if p['addr']==10005)
    case('05 PLC heartbeat writes zero and scanner heartbeat produces no business log',heartbeat)
    seed('REPEAT-VALID',1,500,True)
    case('repeat below maximum does not count but notifies and reads',lambda:scan_check('REPEAT-VALID',2,6,status='WARNING',result='SCAN_REPEATED'))
    case('TPL envelope barcode',lambda:scan_check('TPL-VALID',2,6,tpl=True))
    def split():
        qr='SPLIT-扫码';b=frame(qr);scanner.send(b[:5]);time.sleep(.25);scanner.send(b[5:-2]);time.sleep(.25);scanner.send(b[-2:]);finished(qr)
        assert len(summaries(qr))==1
        assert not sql("SELECT * FROM cushion_info WHERE qr_code='SPL'")
    case('TCP split frame including UTF8 bytes produces one complete code',split)
    def joined():
        scanner.send(frame('JOIN-A')+frame('JOIN-B'));finished('JOIN-A');finished('JOIN-B')
        assert not sql("SELECT * FROM cushion_info WHERE qr_code='JOIN-AJOIN-B'")
    case('TCP coalesced frames produce two separate operations',joined)
    case('barcode containing NoRead text remains a barcode',lambda:scan_check('PART-NoRead-001',2,6))
    case('barcode containing HeartBeat text remains a barcode',lambda:scan_check('PART-HeartBeat-001',2,6))
    for value in [0,32767]:
        plc.value=value;case('read boundary '+str(value),lambda v=value:scan_check('READ-'+str(v),2,6))
    plc.value=-1;case('negative open count rejected without saving',lambda:scan_check('READ-NEGATIVE',2,6,status='WARNING',read_code='PLC_READ_FAILED'));plc.value=42
    plc.mode='READ_REJECT';case('read protocol rejection preserves count and records warning',lambda:scan_check('READ-REJECT',2,6,status='WARNING',read_code='PLC_READ_FAILED'));plc.mode='NORMAL'
    plc.mode='REJECT';case('write rejection prevents read and preserves count',lambda:scan_check('WRITE-REJECT',2,status='FAILED'));plc.mode='NORMAL'
    def missing_read():
        sql('DELETE FROM plc_addr WHERE id=1006')
        try:scan_check('NO-READ-ADDR',2,status='WARNING',read_code='PLC_READ_ADDRESS_NOT_CONFIGURED')
        finally:sql("INSERT INTO plc_addr(id,scanner_id,plc_id,type,addr) VALUES(1006,101,103,6,'MW10006')")
    case('missing read configuration warns and never guesses address',missing_read)
    def mixed_plc():
        sql('UPDATE plc_addr SET plc_id=104 WHERE id=1006')
        try:scan_check('MIXED-PLC',2,status='WARNING',read_code='PLC_READ_ADDRESS_NOT_CONFIGURED')
        finally:sql('UPDATE plc_addr SET plc_id=103 WHERE id=1006')
    case('inconsistent imported PLC association never reads from wrong PLC',mixed_plc)
    def missing_write():
        sql('DELETE FROM plc_addr WHERE id=1002');start=len(plc.packets)
        try:
            scanner.send(frame('NO-WRITE-ADDR'));row=finished('NO-WRITE-ADDR')[0]
            assert row['status']=='WARNING' and json.loads(row['detail_json'])['plcCode']=='PLC_ADDRESS_NOT_CONFIGURED',row
            assert not biz_packets(start)
        finally:sql("INSERT INTO plc_addr(id,scanner_id,plc_id,type,addr) VALUES(1002,101,103,2,'MW10002')")
    case('missing write configuration counts but emits no guessed PLC command',missing_write)
    def offline():
        sql('UPDATE plc_addr SET plc_id=999999 WHERE id=1002');start=len(plc.packets)
        try:
            scanner.send(frame('OFFLINE-PLC'));row=finished('OFFLINE-PLC')[0]
            assert row['status']=='FAILED' and json.loads(row['detail_json'])['plcCode']=='PLC_OFFLINE',row
            assert not biz_packets(start)
        finally:sql('UPDATE plc_addr SET plc_id=103 WHERE id=1002')
    case('offline target produces one failed summary without touching another PLC',offline)
    def rollback():
        sql("CREATE TRIGGER acceptance_reject_detail BEFORE INSERT ON cushion_detail FOR EACH ROW BEGIN IF NEW.qr_code='ROLLBACK-SCAN' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='acceptance detail insert failure'; END IF; END")
        start=len(plc.packets)
        try:
            client.manual('ROLLBACK-SCAN');row=finished('ROLLBACK-SCAN')[0]
            assert row['result_code']=='SCAN_COUNT_FAILED' and row['status']=='FAILED',row
            assert not sql("SELECT id FROM cushion_info WHERE qr_code='ROLLBACK-SCAN'")
            assert not biz_packets(start)
        finally:sql('DROP TRIGGER acceptance_reject_detail')
    case('detail insertion failure rolls back count and never submits PLC command',rollback)
    def read_rollback():
        sql("CREATE TRIGGER acceptance_reject_read BEFORE UPDATE ON cushion_detail FOR EACH ROW BEGIN IF NEW.qr_code='ROLLBACK-READ' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='acceptance detail update failure'; END IF; END")
        try:
            scan_check('ROLLBACK-READ',2,6,status='WARNING',read_code='PLC_READ_FAILED')
            assert sql("SELECT open_count FROM cushion_info WHERE qr_code='ROLLBACK-READ'")[0]['open_count']==0
            assert sql("SELECT open_count FROM cushion_detail WHERE qr_code='ROLLBACK-READ'")[0]['open_count']==0
        finally:sql('DROP TRIGGER acceptance_reject_read')
    case('read persistence exception finalizes log and rolls back both open counts',read_rollback)
    def tied_details():
        seed('TIED-DETAILS',1,500,True)
        sql("INSERT INTO cushion_detail(qr_code,created_date,open_count) SELECT qr_code,created_date,9 FROM cushion_detail WHERE qr_code='TIED-DETAILS' LIMIT 1")
        scan_check('TIED-DETAILS',2,6,status='WARNING',result='SCAN_REPEATED')
        values=sql("SELECT open_count FROM cushion_detail WHERE qr_code='TIED-DETAILS' ORDER BY id")
        assert [r['open_count'] for r in values]==[0,42],values
    case('equal timestamps update latest detail by id rather than older history',tied_details)
    def missing_detail():
        seed('NO-HISTORY',1,500,True,False)
        scan_check('NO-HISTORY',2,6,status='WARNING',result='SCAN_REPEATED',read_code='PLC_READ_FAILED')
        assert sql("SELECT open_count FROM cushion_info WHERE qr_code='NO-HISTORY'")[0]['open_count']==0
    case('missing detail warns without leaving partial open-count update',missing_detail)
    def ambiguous_manual():
        sql("INSERT INTO device_info(id,type,name,ip,port,work_line,install_seq) VALUES(102,0,'another scanner','127.0.0.1',%s,1,2)",(scanner.port,));start=len(plc.packets)
        try:
            client.manual('AMBIGUOUS-MANUAL');row=finished('AMBIGUOUS-MANUAL')[0]
            assert row['status']=='WARNING' and json.loads(row['detail_json'])['plcCode']=='PLC_TARGET_NOT_RESOLVED',row
            assert not biz_packets(start)
        finally:sql('DELETE FROM device_info WHERE id=102')
    case('new manual scan with multiple scanners never broadcasts',ambiguous_manual)
    def concurrent_existing():
        seed('CONCURRENT-OLD',1,500)
        def request(i):
            c=Client();c.call('POST','/auth/login',{'username':'superadmin','password':'example-maintenance-password-1'});return c.manual('CONCURRENT-OLD')
        with concurrent.futures.ThreadPoolExecutor(6) as ex:list(ex.map(request,range(6)))
        rows=finished('CONCURRENT-OLD',6);assert len(rows)==6
        assert sql("SELECT used_count FROM cushion_info WHERE qr_code='CONCURRENT-OLD'")[0]['used_count']==2
        assert sum(r['result_code']=='SCAN_COUNTED' for r in rows)==1,[(r['result_code'],r['status']) for r in rows]
    case('six simultaneous scans of existing cushion count only once',concurrent_existing)
    def concurrent_new():
        def request(i):
            c=Client();c.call('POST','/auth/login',{'username':'superadmin','password':'example-maintenance-password-1'});return c.manual('CONCURRENT-NEW')
        with concurrent.futures.ThreadPoolExecutor(6) as ex:list(ex.map(request,range(6)))
        rows=finished('CONCURRENT-NEW',6)
        assert len(rows)==6 and all(r['status'] in ('SUCCESS','WARNING') for r in rows),[(r['result_code'],r['status']) for r in rows]
        assert sql("SELECT used_count FROM cushion_info WHERE qr_code='CONCURRENT-NEW'")==[{'used_count':1}]
    case('six simultaneous first scans create once and none fail as duplicate key',concurrent_new)
    def concurrent_max():
        seed('CONCURRENT-MAX',1,2);start=len(plc.packets)
        def request(i):
            c=Client();c.call('POST','/auth/login',{'username':'superadmin','password':'example-maintenance-password-1'});return c.manual('CONCURRENT-MAX')
        with concurrent.futures.ThreadPoolExecutor(6) as ex:list(ex.map(request,range(6)))
        rows=finished('CONCURRENT-MAX',6)
        assert all(r['result_code']=='CUSHION_MAX_REACHED' for r in rows),[(r['result_code'],r['status']) for r in rows]
        assert sql("SELECT used_count FROM cushion_info WHERE qr_code='CONCURRENT-MAX'")[0]['used_count']==2
        assert [(p['fc'],p['addr']) for p in biz_packets(start)]==[(6,10003)]*6
    case('concurrent crossing of maximum never emits success from stale data',concurrent_max)
    def same_operation():
        op=str(uuid.uuid4());client.manual('IDEMPOTENT',op);finished('IDEMPOTENT');start=len(plc.packets);client.manual('IDEMPOTENT',op);time.sleep(.3)
        assert len(summaries('IDEMPOTENT'))==1 and not biz_packets(start)
    case('same operation id never replays count or PLC write',same_operation)
    def events():
        row=summaries('AUTO-SUCCESS')[0];codes=[r['code'] for r in sql('SELECT code FROM operation_event WHERE operation_id=%s',(row['operation_id'],))]
        assert {'SCAN_COUNTED','PLC_NOTIFY_PENDING','PLC_NOTIFY_SUCCEEDED','PLC_READ_SUCCEEDED'}<=set(codes),codes
    case('all stages persist operation events after business commit',events)
    def sse_check():
        op=summaries('AUTO-SUCCESS')[0]['operation_id']
        def found():
            codes=[]
            for message in sse_events:
                envelope=message.get('data') or {};event=envelope.get('data') or {}
                if isinstance(event,dict) and event.get('operationId')==op:codes.append(event.get('code'))
            return {'SCAN_COUNTED','PLC_NOTIFY_SUCCEEDED','PLC_READ_SUCCEEDED'}<=set(codes)
        wait(found,3)
    case('live SSE delivers correlated scan write and read results',sse_check)
    # Isolate business timeout tests from the existing periodic heartbeat schedule.
    sql('DELETE FROM plc_addr WHERE id=1005');time.sleep(.2)
    plc.mode='SILENT'
    case('actual unanswered write times out once and never reads',lambda:scan_check('WRITE-TIMEOUT',2,status='FAILED'))
    plc.mode='NORMAL'
    case('valid business response recovers after write timeout',lambda:scan_check('WRITE-RECOVERY',2,6))
    plc.mode='READ_SILENT'
    case('actual unanswered read finalizes warning without replaying write',lambda:scan_check('READ-TIMEOUT',2,6,status='WARNING',read_code='PLC_READ_FAILED'))
    plc.mode='NORMAL'
    case('valid read recovers after read timeout',lambda:scan_check('READ-RECOVERY',2,6))
    plc.mode='CLOSE'
    case('TCP reset during write records failure without count rollback or read',lambda:scan_check('WRITE-RESET',2,status='FAILED'))
    plc.mode='NORMAL'
    case('untargeted PLC receives no operation',lambda:(_ for _ in ()).throw(AssertionError(other.packets)) if other.packets else None)
    report['scanLogs']=sql('SELECT operation_id,qr_code,status,result_code,msg,detail_json FROM scan_log ORDER BY id')
    report['plcPackets']=plc.packets;report['otherPlcPackets']=other.packets;report['sseEvents']=sse_events
    report['status']='PASS' if all(c['status']=='PASS' for c in report['cases']) else 'FAIL'
finally:
    (run/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2,default=str),encoding='utf-8')
    for p in [scanner,plc,other]:p.close()
    if backend:
        backend.terminate()
        try:backend.wait(timeout=15)
        except subprocess.TimeoutExpired:backend.kill();backend.wait()
    if sse_response:sse_response.close()
    if mysql:
        subprocess.run([str(mysql_bin/'mysqladmin.exe'),'--protocol=TCP','--host=127.0.0.1','--port='+str(db_port),'-uroot','shutdown'],capture_output=True,creationflags=flags)
        try:mysql.wait(timeout=20)
        except subprocess.TimeoutExpired:mysql.terminate();mysql.wait()
    for f in logs:f.close()
print('ACCEPTANCE_'+report['status'],flush=True)
sys.exit(0 if report['status']=='PASS' else 1)
