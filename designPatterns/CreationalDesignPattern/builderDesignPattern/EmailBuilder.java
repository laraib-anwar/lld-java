@Getter
public class EmailBuilder {
    private tring to;
    private String subject;
    private String body;
    private String cc;
    private String bcc;
    private List<String> attachments = new ArrayList<>();
 
    public EmailBuilder setTo(String to) {
        this.to = to;
        return this;
    }
 
    public EmailBuilder setSubject(String subject) {
        this.subject = subject;
        return this;
    }
 
    public EmailBuilder setBody(String body) {
        this.body = body;
        return this;
    }
 
    public EmailBuilder setCc(String cc) {
        this.cc = cc;
        return this;
    }
 
    public EmailBuilder setBcc(String bcc) {
        this.bcc = bcc;
        return this;
    }
 
    public EmailBuilder addAttachment(String attachment) {
        this.attachments.add(attachment);
        return this;
    }
 
    public Email build() { 
        if (to == null) { 
            throw new IllegalStateException("To is a required field."); 
        } 
        return new Email(this); 
    } 
}