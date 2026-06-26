public class XmlServiceAdapter implements UserService {
    private LegacyXmlService legacyXmlService;
 
    public XmlServiceAdapter(LegacyXmlService legacyXmlService) {
        this.legacyXmlService = legacyXmlService;
    }
 
    @Override
    public String fetchData() {
        String xmlData = legacyXmlService.fetchData();
        // convert to json
        return "{ \"name\": \"John Doe\", \"age\": 30 }";  // JSON response
    }
}