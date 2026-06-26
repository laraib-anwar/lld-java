class PDFDocument extends PrintableDocument {
    @Override
    public void open(String filename) {
        System.out.println("Opening the PDF Document");
    }
 
    @Override
    public void save(String filename) {
        System.out.println("Saving the PDF Document");
    }
 
    @Override
    public void print(String filename) {
        System.out.println("Printing the PDF Document");
    }
}