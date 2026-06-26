public class Main {
    public static void main(String[] args) {
        String candidate = "Alice";
 
        TechnicalInterview technicalInterview = new TechnicalInterview();
        if(!technicalInterview.hire(candidate)) return;
 
        BarRaiserInterview barraiserInterview = new BarRaiserInterview();
        if(!barraiserInterview.hire(candidate)) return;
 
        HRInterview hrInterview = new HRInterview();
        if(!hrInterview.hire(candidate)) return;
 
        System.out.println(candidate + " passed all interviews!");
