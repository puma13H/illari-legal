"""Genera la guía incluida en el recurso digital de bienvenida del catálogo."""
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, white
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle
import reportlab

fonts=Path(reportlab.__file__).parent/'fonts'
pdfmetrics.registerFont(TTFont('PortalSans',str(fonts/'Vera.ttf')))
pdfmetrics.registerFont(TTFont('PortalBold',str(fonts/'VeraBd.ttf')))
out=ROOT/'backend/microservicios/pedidos/src/main/resources/guias/guia-bienvenida.pdf'
out.parent.mkdir(parents=True,exist_ok=True)
c=canvas.Canvas(str(out),pagesize=(595.28,841.89))
c.setTitle('Guía de bienvenida digital | ILLARI LEGAL')
c.setAuthor('ILLARI LEGAL')
navy=HexColor('#263f57');gold=HexColor('#bc9f66');muted=HexColor('#657b8d')
body=ParagraphStyle('Body',fontName='PortalSans',fontSize=10.4,leading=17,textColor=muted)
def paragraph(text,x,y,width,style=body):
    p=Paragraph(text,style);_,h=p.wrap(width,700);p.drawOn(c,x,y-h);return y-h
def footer(page):
    c.setStrokeColor(HexColor('#dce4ec'));c.line(46,53,549,53)
    c.setFillColor(muted);c.setFont('PortalSans',8);c.drawString(46,35,'ILLARI LEGAL / Guía de uso del portal');c.drawRightString(549,35,str(page))
def block(number,title,text,y):
    c.setFillColor(gold);c.setFont('PortalBold',11);c.drawString(46,y,number)
    c.setFillColor(navy);c.setFont('PortalBold',13);c.drawString(80,y,title)
    return paragraph(text,80,y-15,455)-30

c.setFillColor(navy);c.rect(0,625,596,218,fill=1,stroke=0)
image=ROOT/'frontend/portal-web/src/assets/images/portada-asesoria-1200.jpg'
c.saveState();c.setFillAlpha(.22);c.drawImage(ImageReader(str(image)),0,625,595,218,mask='auto',preserveAspectRatio=False);c.restoreState()
c.setFillColor(HexColor('#e6d0a7'));c.setFont('PortalBold',9);c.drawString(46,794,'ILLARI LEGAL  /  RECURSO DIGITAL')
c.setFillColor(white);c.setFont('Times-Roman',32);c.drawString(46,736,'Guía de bienvenida')
c.setFont('PortalSans',11);c.drawString(46,701,'Tu cuenta, tus servicios y el avance de tu atención.')
paragraph('Una guía breve para encontrar lo que necesitas, organizar tus documentos y aprovechar los espacios de tu cuenta personal.',46,589,503)
y=block('01','Ingresa a tu espacio personal','Accede desde el enlace del portal y selecciona Iniciar sesión. Usa el usuario que te entregó el estudio. Si es tu primer ingreso, el sistema puede solicitar una contraseña nueva.',505)
y=block('02','Encuentra el servicio que necesitas','En Servicios puedes buscar por nombre, filtrar por categoría y revisar el precio. Agrega uno o varios servicios a tu pedido. El resumen muestra cada concepto y el importe total antes de continuar.',y)
y=block('03','Revisa tus pedidos y pagos','Mis pedidos reúne el historial de tu cuenta. Desde allí puedes consultar un pedido, retomar un pago pendiente y revisar una confirmación. Comprueba el importe antes de autorizar un pago en Yape.',y)
footer(1);c.showPage()
c.setFillColor(navy);c.setFont('PortalBold',10);c.drawString(46,790,'TU ATENCIÓN, ORGANIZADA')
c.setFont('Times-Roman',29);c.drawString(46,748,'Todo a la mano.')
paragraph('El portal conecta la información de tu atención con el trabajo del profesional responsable.',46,714,503)
y=block('04','Consulta Mis expedientes','Los asuntos asignados a tu cuenta aparecen en Mis expedientes. Al abrir uno puedes consultar su estado actual, la fecha de apertura, la próxima audiencia registrada y los datos del profesional responsable.',641)
y=block('05','Organiza tus documentos','Los documentos incorporados por el equipo se muestran dentro de cada expediente. Selecciona Descargar para guardar una copia. Identifica tus archivos por asunto y fecha para encontrarlos con facilidad.',y)
y=block('06','Mantén tu acceso personal','Conserva tu contraseña y tus códigos de aprobación para tu uso personal. Si necesitas corregir tus datos de contacto, solicita la actualización al administrador del estudio. Los expedientes de referencia están identificados en el portal.',y)
c.setFillColor(HexColor('#f5f7fa'));c.roundRect(46,142,503,104,9,fill=1,stroke=0)
paragraph('<b>Sobre este recurso</b><br/>Esta guía explica el uso de ILLARI LEGAL. Los servicios de asesoría se eligen y contratan por separado desde el catálogo. Las fechas y el estado de cada expediente corresponden a la información registrada por el equipo responsable.',64,226,467)
footer(2);c.save()
print(out)
