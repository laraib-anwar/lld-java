class WordDocument extends PrintableDocument {
    @Override
    public void open(String filename) {
        System.out.println("Opening the Word Document");
    }
 
    @Override
    public void save(String filename) {
        System.out.println("Saving the Word Document");
    }
 
    @Override
    public void print(String filename) {
        System.out.println("Printing the Word Document");
    }
}