public class Main {
    public static void main(String[] args){
        PrintableDocument pdf = new PDFDocument();
        pdf.open("Resume.pdf");
        pdf.print("Resume.pdf");
 
        PrintableDocument word = new WordDocument();
        word.open("Report.docx");
        word.print("Report.docx");
 
        Document text = new TextDocument();
        text.open("Notes.txt");
        text.save("Notes.txt");
        // ❌ Not allowed: text.print() — it doesn't have print() anymore
    }
}