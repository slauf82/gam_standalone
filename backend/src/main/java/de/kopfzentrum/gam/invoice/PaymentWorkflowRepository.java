package de.kopfzentrum.gam.invoice;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class PaymentWorkflowRepository {
 private final JdbcTemplate jdbc;
 public PaymentWorkflowRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @PostConstruct void ensureSchema(){
  jdbc.execute("""
      CREATE TABLE IF NOT EXISTS payment_workflow_state (
        invoice_number VARCHAR(80) NOT NULL,
        company_id INT NULL,
        status VARCHAR(32) NOT NULL DEFAULT 'OFFEN',
        amount_due DECIMAL(15,2) NOT NULL DEFAULT 0,
        paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
        due_date DATE NULL,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        updated_by VARCHAR(255) NULL,
        PRIMARY KEY(invoice_number, company_id)
      )
      """);
  jdbc.execute("""
      CREATE TABLE IF NOT EXISTS payment_workflow_history (
        id BIGINT NOT NULL AUTO_INCREMENT,
        invoice_number VARCHAR(80) NOT NULL,
        company_id INT NULL,
        from_status VARCHAR(32) NULL,
        to_status VARCHAR(32) NOT NULL,
        amount DECIMAL(15,2) NULL,
        note TEXT NULL,
        changed_by VARCHAR(255) NULL,
        changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(id),
        INDEX idx_payment_workflow_history(invoice_number, company_id, changed_at)
      )
      """);
  jdbc.execute("""
      CREATE TABLE IF NOT EXISTS payment_workflow_document (
        id BIGINT NOT NULL AUTO_INCREMENT,
        invoice_number VARCHAR(80) NOT NULL,
        company_id INT NULL,
        document_type VARCHAR(32) NOT NULL,
        title VARCHAR(255) NOT NULL,
        language VARCHAR(8) NOT NULL DEFAULT 'de',
        pdf_data LONGBLOB NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(255) NULL,
        PRIMARY KEY(id),
        INDEX idx_payment_document_invoice(invoice_number, company_id, created_at)
      )
      """);
 }
 @Transactional public PaymentWorkflowState getOrCreate(String number,Integer companyId,BigDecimal amountDue,LocalDate dueDate,String user){
  Integer c=jdbc.queryForObject("SELECT COUNT(*) FROM payment_workflow_state WHERE invoice_number=? AND company_id <=> ?",Integer.class,number,companyId);
  if(c==null||c==0){jdbc.update("INSERT INTO payment_workflow_state(invoice_number,company_id,status,amount_due,due_date,updated_by) VALUES(?,?,'OFFEN',?,?,?)",number,companyId,nz(amountDue),dueDate,user);jdbc.update("INSERT INTO payment_workflow_history(invoice_number,company_id,from_status,to_status,note,changed_by) VALUES(?,?,NULL,'OFFEN','Zahlungsworkflow initialisiert',?)",number,companyId,user);} else if(amountDue!=null&&amountDue.signum()>=0){jdbc.update("UPDATE payment_workflow_state SET amount_due=?,due_date=COALESCE(?,due_date) WHERE invoice_number=? AND company_id <=> ?",amountDue,dueDate,number,companyId);} return load(number,companyId);
 }
 @Transactional public PaymentWorkflowState update(String number,Integer companyId,PaymentWorkflowUpdateRequest r,String user){
  PaymentWorkflowState cur=getOrCreate(number,companyId,r.amountDue(),r.dueDate(),user); String action=(r.action()==null?"":r.action().trim().toUpperCase(Locale.ROOT)); BigDecimal paid=cur.paidAmount(); String target=cur.status(); BigDecimal booking=nz(r.amount());
  switch(action){case "ZAHLUNG" -> {if(booking.signum()<=0) throw new IllegalArgumentException("Zahlungsbetrag muss groesser als 0 sein."); paid=paid.add(booking); target=paid.compareTo(cur.amountDue())>=0?"BEZAHLT":"TEILZAHLUNG";} case "BEZAHLT" -> {paid=cur.amountDue(); booking=cur.openAmount(); target="BEZAHLT";} case "ZAHLUNGSERINNERUNG" -> target="ZAHLUNGSERINNERUNG"; case "MAHNUNG_1" -> target="MAHNUNG_1"; case "MAHNUNG_2" -> target="MAHNUNG_2"; case "MAHNUNG_3" -> target="MAHNUNG_3"; case "INKASSO" -> target="INKASSO"; case "AUFSCHUB_1_TAG", "AUFSCHUB_3_TAGE", "AUFSCHUB_7_TAGE" -> { int days = action.equals("AUFSCHUB_1_TAG") ? 1 : action.equals("AUFSCHUB_7_TAGE") ? 7 : 3; LocalDate base=cur.dueDate()==null?LocalDate.now():cur.dueDate(); LocalDate next=base.plusDays(days); jdbc.update("UPDATE payment_workflow_state SET due_date=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE invoice_number=? AND company_id <=> ?",next,user,number,companyId); jdbc.update("INSERT INTO payment_workflow_history(invoice_number,company_id,from_status,to_status,note,changed_by) VALUES(?,?,?,?,?,?)",number,companyId,cur.status(),cur.status(),"Fristaufschub um "+days+" Tag(e): "+base+" → "+next,user); return load(number,companyId);} case "STORNIERT" -> target="STORNIERT"; case "ZURUECKSETZEN" -> {paid=BigDecimal.ZERO; booking=BigDecimal.ZERO; target="OFFEN";} default -> throw new IllegalArgumentException("Unbekannte Zahlungsaktion: "+r.action());}
  jdbc.update("UPDATE payment_workflow_state SET status=?,paid_amount=?,amount_due=COALESCE(?,amount_due),due_date=COALESCE(?,due_date),updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE invoice_number=? AND company_id <=> ?",target,paid,r.amountDue(),r.dueDate(),user,number,companyId);
  jdbc.update("INSERT INTO payment_workflow_history(invoice_number,company_id,from_status,to_status,amount,note,changed_by) VALUES(?,?,?,?,?,?,?)",number,companyId,cur.status(),target,booking,r.note(),user); return load(number,companyId);
 }
 private PaymentWorkflowState load(String n,Integer c){var m=jdbc.queryForMap("SELECT * FROM payment_workflow_state WHERE invoice_number=? AND company_id <=> ?",n,c);BigDecimal due=(BigDecimal)m.get("amount_due"),paid=(BigDecimal)m.get("paid_amount");String status=String.valueOf(m.get("status"));Timestamp ts=(Timestamp)m.get("updated_at");List<PaymentWorkflowEntry> h=jdbc.query("SELECT * FROM payment_workflow_history WHERE invoice_number=? AND company_id <=> ? ORDER BY changed_at DESC,id DESC",(rs,row)->new PaymentWorkflowEntry(rs.getLong("id"),rs.getString("invoice_number"),(Integer)rs.getObject("company_id"),rs.getString("from_status"),rs.getString("to_status"),rs.getBigDecimal("amount"),rs.getString("note"),rs.getString("changed_by"),rs.getTimestamp("changed_at").toLocalDateTime()),n,c);List<String>a="STORNIERT".equals(status)?List.of("ZURUECKSETZEN"):"BEZAHLT".equals(status)?List.of("ZURUECKSETZEN"):List.of("ZAHLUNG","BEZAHLT","ZAHLUNGSERINNERUNG","MAHNUNG_1","MAHNUNG_2","MAHNUNG_3","INKASSO","STORNIERT");return new PaymentWorkflowState(n,c,status,due,paid,due.subtract(paid).max(BigDecimal.ZERO),m.get("due_date") instanceof java.sql.Date d ? d.toLocalDate() : null,ts==null?LocalDateTime.now():ts.toLocalDateTime(),String.valueOf(m.get("updated_by")),a,h);}

 @Transactional public PaymentDocument saveDocument(String number,Integer companyId,String type,String title,String language,byte[] pdf,String user){
  jdbc.update("INSERT INTO payment_workflow_document(invoice_number,company_id,document_type,title,language,pdf_data,created_by) VALUES(?,?,?,?,?,?,?)",number,companyId,type,title,language,pdf,user);
  Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class); return document(id==null?0:id);
 }
 public PaymentDocument document(long id){return jdbc.queryForObject("SELECT id,invoice_number,company_id,document_type,title,language,created_at,created_by FROM payment_workflow_document WHERE id=?",(rs,row)->new PaymentDocument(rs.getLong(1),rs.getString(2),(Integer)rs.getObject(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getTimestamp(7).toLocalDateTime(),rs.getString(8)),id);}
 public byte[] documentPdf(long id){return jdbc.queryForObject("SELECT pdf_data FROM payment_workflow_document WHERE id=?",byte[].class,id);}
 public void updateDocumentPdf(long id, byte[] pdf){jdbc.update("UPDATE payment_workflow_document SET pdf_data=? WHERE id=?",pdf,id);}
 public boolean hasDocumentType(String number,Integer companyId,String type){Integer c=jdbc.queryForObject("SELECT COUNT(*) FROM payment_workflow_document WHERE invoice_number=? AND company_id <=> ? AND document_type=?",Integer.class,number,companyId,type);return c!=null&&c>0;}
 public List<PaymentDocument> documents(String number,Integer companyId){return jdbc.query("SELECT id,invoice_number,company_id,document_type,title,language,created_at,created_by FROM payment_workflow_document WHERE invoice_number=? AND company_id <=> ? ORDER BY created_at DESC,id DESC",(rs,row)->new PaymentDocument(rs.getLong(1),rs.getString(2),(Integer)rs.getObject(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getTimestamp(7).toLocalDateTime(),rs.getString(8)),number,companyId);}
 public PaymentDocument latestDocument(String number,Integer companyId){List<PaymentDocument> docs=documents(number,companyId);return docs.isEmpty()?null:docs.get(0);}
 private BigDecimal nz(BigDecimal x){return x==null?BigDecimal.ZERO:x;}
}
