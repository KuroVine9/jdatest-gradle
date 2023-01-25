package kuro9.mahjongbot;

import kuro9.mahjongbot.instruction.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class Main extends ListenerAdapter {
    private static RestAction<User> ADMIN;
    private static EntireRank entireRank;
    private static MonthRank monthRank;
    private static SeasonRank seasonRank;
    private static EntireStat entireStat;
    private static MonthStat monthStat;
    private static SeasonStat seasonStat;

    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        System.out.println("[MahjongBot:Main] System Initializing...");
        Setting.init();
        final String TOKEN;
        try {

            Scanner scan = new Scanner(new File(Setting.TOKEN_PATH));
            TOKEN = scan.next();
            scan.close();
        } catch (IOException e) {
            System.out.println("\n\n[MahjongBot:Main] Initialize Failure!\n\n");
            throw new RuntimeException(e);
        }
        JDA jda = JDABuilder.createDefault(TOKEN).build();
        ADMIN = jda.retrieveUserById(Setting.ADMIN_ID);
        jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
        jda.retrieveUserById(Setting.ADMIN_ID).map(User::getAsTag)
                .queue(name -> jda.getPresence().setActivity(Activity.competing("DM => " + name))
                );

        jda.addEventListener(new Main());

        System.out.println("[MahjongBot:Main] Initialize Complete!\n");

        System.out.println("[MahjongBot:Main] Loading Instructions...");
        CommandListUpdateAction commands = jda.updateCommands();
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(Setting.INST_PATH));
            JSONArray jsonArray = (JSONArray) obj;
            jsonArray.stream().peek(
                    data -> System.out.printf("[MahjongBot:Main] Loaded Instruction \"%s\"\n", ((JSONObject) data).get("name"))
            ).forEach(data -> commands.addCommands(CommandData.fromData(DataObject.fromJson(data.toString()))));
        } catch (IOException | ParseException e) {
            System.out.println("\n\n[MahjongBot:Main] Runtime Instruction Loading Failure!\n\n");
            Logger.addSystemErrorEvent("instruction-load-err", ADMIN);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        commands.queue();
        entireRank = new EntireRank();
        monthRank = new MonthRank();
        seasonRank = new SeasonRank();
        entireStat = new EntireStat();
        monthStat = new MonthStat();
        seasonStat = new SeasonStat();
        System.out.println("[MahjongBot:Main] Instructions Loaded!");

        System.out.printf("[MahjongBot:Main] Bot Started! (%d ms)\n", System.currentTimeMillis() - time);
        Logger.addSystemEvent("system-start");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.printf("[DM] %s: %s\n", event.getAuthor().getName(), event.getMessage().getContentDisplay());

        }
        else
            System.out.printf("[%s] [%s] %s: %s\n", event.getGuild().getName(), event.getChannel().getName()
                    , event.getMember().getEffectiveName(), event.getMessage().getContentDisplay());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.printf("[MahjongBot:Main] %s used /%s\n", event.getUser().getAsTag(), event.getName());
        switch (event.getName()) {
            case "msg" -> {
                ADMIN.queue(
                        admin -> admin.openPrivateChannel().queue(
                                privateChannel -> privateChannel.sendMessage("test").queue()
                        )
                );
            }
            case "ping" -> {
                long time = System.currentTimeMillis();
                event.reply("Pong!").setEphemeral(true)
                        .flatMap(
                                v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)
                        ).queue();
            }
            case "name" -> {
                event.reply(
                        String.format("UserName: %s", event.getOption("user").getAsUser().getAsTag())
                ).addActionRow(
                        Button.primary("buttonID", "buttonName")
                ).queue();
            }
            case "add" -> Add.action(event, ADMIN);
            case "stat" -> seasonStat.action(event);
            case "month_stat" -> monthStat.action(event);
            case "entire_stat" -> entireStat.action(event);
            case "revalid" -> ReValid.action(event, ADMIN);
            case "entire_rank" -> {
                switch (event.getOption("type") == null ? -1 : (int) event.getOption("type").getAsLong()) {
                    case 0 -> entireRank.summaryReply(event);
                    case 1, -1 -> entireRank.umaReply(event);
                    case 2 -> entireRank.totalGameReply(event);
                }
            }
            case "month_rank" -> {
                switch (event.getOption("type") == null ? -1 : (int) event.getOption("type").getAsLong()) {
                    case 0 -> monthRank.summaryReply(event);
                    case 1, -1 -> monthRank.umaReply(event);
                    case 2 -> monthRank.totalGameReply(event);
                }
            }
            case "rank" -> {
                switch (event.getOption("type") == null ? -1 : (int) event.getOption("type").getAsLong()) {
                    case 0 -> seasonRank.summaryReply(event);
                    case 1, -1 -> seasonRank.umaReply(event);
                    case 2 -> seasonRank.totalGameReply(event);
                }
            }

            default -> throw new IllegalStateException("Unexpected value: " + event.getName());
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        System.out.printf("[MahjongBot:Main] %s used BUTTON.%s\n", event.getUser().getAsTag(), event.getComponentId());

        if (event.getComponentId().matches("^rank_uma.*")) entireRank.umaPageControl(event);
        else if (event.getComponentId().matches("^rank_totalgame.*")) entireRank.totalGamePageControl(event);
        else if (event.getComponentId().matches("^month_rank_uma.*")) monthRank.umaPageControl(event);
        else if (event.getComponentId().matches("^month_rank_totalgame.*")) monthRank.totalGamePageControl(event);
        else if (event.getComponentId().matches("^season_rank_uma.*")) seasonRank.umaPageControl(event);
        else if (event.getComponentId().matches("^season_rank_totalgame.*")) seasonRank.totalGamePageControl(event);
        else if (event.getComponentId().equals("buttonID")) event.editMessage("button!").queue();
    }

}