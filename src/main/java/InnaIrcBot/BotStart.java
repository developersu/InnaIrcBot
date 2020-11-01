package InnaIrcBot;

import InnaIrcBot.config.ConfigurationFileGenerator;
import org.apache.commons.cli.*;

public class BotStart {
    public BotStart(String[] args){

        final Options cliOptions = createCliOptions();
        CommandLineParser cliParser = new DefaultParser();

        try{
            CommandLine cli = cliParser.parse(cliOptions, args);
            if (cli.hasOption('v') || cli.hasOption("version")){
                handleVersion();
                return;
            }
            if (cli.hasOption("c") || cli.hasOption("configuration")){
                final String[] arguments = cli.getOptionValues("configuration");
                for (String a: arguments)
                ConnectionsBuilder.buildConnections(arguments);
                return;
            }
            if (cli.hasOption("g") || cli.hasOption("generate")){
                final String[] arguments = cli.getOptionValues("generate");
                handleGenerate(arguments);
                return;
            }

            handleHelp(cliOptions);
        }
        catch (ParseException pe){
            handleHelp(cliOptions);
        }
        catch (Exception e){
            System.out.println("Error: ");
            e.printStackTrace();
        }
    }
    private Options createCliOptions(){
        final Options options = new Options();

        final Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Show this help")
                .hasArg(false)
                .build();

        final Option versionOption = Option.builder("v")
                .longOpt("version")
                .desc("Show application version")
                .hasArg(false)
                .build();

        final Option configurationOption = Option.builder("c")
                .longOpt("configuration")
                .desc("Start with configuration")
                .hasArgs()
                .build();

        final Option generateOption = Option.builder("g")
                .longOpt("generate")
                .desc("Create configuration template")
                .hasArg(true)
                .numberOfArgs(1)
                .build();

        final OptionGroup group = new OptionGroup();
        group.addOption(helpOption);
        group.addOption(versionOption);
        group.addOption(configurationOption);
        group.addOption(generateOption);

        options.addOptionGroup(group);

        return options;
    }

    private void handleVersion(){
        System.out.println(GlobalData.getAppVersion());
    }

    private void handleHelp(Options cliOptions){
        new HelpFormatter().printHelp(
                120,
                "InnaIrcBot.jar [OPTION]... [FILE]...",
                "options:",
                cliOptions,
                "\n");
    }
    private void handleGenerate(String[] arguments){
        if (arguments.length > 0)
            ConfigurationFileGenerator.generate(arguments[0]);
        else
            ConfigurationFileGenerator.generate(null);
    }
}
