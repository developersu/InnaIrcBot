import InnaIrcBot.Config.StorageFile;

public class StorageFileTest {
    static public void main(String[] args){
        StorageFile config = new StorageFile(
                "",
                0,
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                true,
                "",
                new String[]{null},
                "",
                ""
        );

        System.out.println(config.getLogDriver().isEmpty());
        System.out.println(config.getLogDriverParameters().length);


    }
}
