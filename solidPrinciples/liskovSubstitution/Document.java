class Document {
    private String title;
    private String content;
 
    public void open(String filename) {
        System.out.println("Opening the Document");
    }
 
    public void save(String filename) {
        System.out.println("Saving the Document");
    }
 
    // getters and setters
}