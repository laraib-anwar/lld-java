class TextDocument extends Document {
    @Override
    public void open(String filename) {
        System.out.println("Opening the Text Document");
    }
 
    @Override
    public void save(String filename) {
        System.out.println("Saving the Text Document");
    }
 
    // no need to extend print because parent does not have print()
}