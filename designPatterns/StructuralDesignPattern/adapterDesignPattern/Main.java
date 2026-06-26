public class Main {
    public static void main(String[] args) {
        // Legacy XML Service
        LegacyXmlService legacyXmlService = new LegacyXmlService();
        UserService xmlService = new XmlServiceAdapter(legacyXmlService);
        System.out.println(xmlService.fetchData());
 
        // Modern JSON Service
        ModernJsonService jsonService = new ModernJsonService();
        System.out.println(jsonService.fetchData());
    }
}