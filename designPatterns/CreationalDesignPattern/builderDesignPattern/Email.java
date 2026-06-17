@Getter
public class Email {
    private String to;
    private String subject;
    private String body;
    private String cc;
    private String bcc;
    private List<String> attachments;
 
    // We let the builder initialize the Email Object.
    Email(EmailBuilder builder){ 
        this.to = builder.getTo(); 
        this.subject = builder.getSubject(); 
        this.body = builder.getBody(); 
        this.cc = builder.getCc(); 
        this.bcc = builder.getBcc(); 
        this.attachments = builder.getAttachments(); 
    } 
 
    // Getters only – no setters (immutability)
}