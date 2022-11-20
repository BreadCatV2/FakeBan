package cc.breadcat.fakeban;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

@Mod(modid = FakeBan.MODID, version = FakeBan.VERSION)
public class FakeBan
{
    public static final String MODID = "FakeBan";
    public static final String VERSION = "1.0";

    private Instant timeStamp;
    private String watchDog;
    private String banID;

    private File config;
    private String configContents;

    @EventHandler
    public void init(FMLPreInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        //check if config file exists and create it if it doesn't
        config = new File(event.getModConfigurationDirectory(), "wdtmpstmp.cfg");
        if (config.exists()) {
            //read config file, timestamp is first line, watchdog is second line, banid is third line
            try {
                configContents = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
                String[] configLines = configContents.split("\n");
                timeStamp = Instant.parse(configLines[0]);
                watchDog = configLines[1];
                banID = configLines[2];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //when the player has joined the server open the gui
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!event.manager.getRemoteAddress().toString().contains("hypixel.net")) {
            return;
        }
        if (timeStamp == null || watchDog == null || banID == null) {
            timeStamp = Instant.now();
            watchDog = String.valueOf((char) (Math.random() * 26 + 'A')) + String.valueOf((char) (Math.random() * 26 + 'A')) + "-" + (int) (Math.random() * 1000000);
            banID = RandomStringUtils.random(8, "0123456789ABCDEF");
            try {
                Files.write(config.toPath(), (timeStamp.toString() + "\n" + watchDog + "\n" + banID).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Duration banTime = Duration.between(Instant.now(), timeStamp.plusSeconds(60 * 60 * 24 * 30));
        long seconds = banTime.getSeconds(); long dd = seconds / 86400; long hh = seconds % 86400 / 3600; long mm = seconds % 3600 / 60; long ss = seconds % 60;
        final String banTimeString = (dd > 0 ? dd + "d " : "") + (hh > 0 ? hh + "h " : "") + (mm > 0 ? mm + "m " : "") + (ss + "s");
        Minecraft.getMinecraft().displayGuiScreen(new GuiScreen() {
            @Override
            public void drawScreen(int mouseX, int mouseY, float partialTicks) {
                this.drawDefaultBackground();
                this.drawCenteredString(this.fontRendererObj, "Failed to connect to the server", this.width / 2, this.height / 2 - 50, 0xAAAAAA);
                this.drawCenteredString(this.fontRendererObj, "You are temporarily banned for " + "\u00A7f" + banTimeString + "\u00A7r" + " from this server!", this.width / 2, this.height / 2 - 30, 0xFF5555);
                this.drawCenteredString(this.fontRendererObj, "\u00A7rReason: " + "\u00A7f" + "WATCHDOG CHEAT DETECTION" + "\u00A7r \u00A7o" + "[" + watchDog + "]", this.width / 2, this.height / 2 - 10, 0xAAAAAA);
                this.drawCenteredString(this.fontRendererObj, "Find out more: " + "\u00A7b" + "https://www.hypixel.net/watchdog", this.width / 2, this.height / 2, 0xAAAAAA);
                this.drawCenteredString(this.fontRendererObj, "Ban ID: " + "\u00A7f" + banID, this.width / 2, this.height / 2 + 20, 0xAAAAAA);
                this.drawCenteredString(this.fontRendererObj, "Sharing your Ban ID may affect the processing of your appeal!", this.width / 2, this.height / 2 + 30, 0xAAAAAA);
                super.drawScreen(mouseX, mouseY, partialTicks);
                //add a back button that closes the gui and sends the player to the server selection screen
                this.buttonList.add(new net.minecraft.client.gui.GuiButton(0, this.width / 2 - 100, this.height / 2 + 45, "Back to server list") {
                    @Override
                    public void mouseReleased(int mouseX, int mouseY
                    ) {
                        Minecraft.getMinecraft().displayGuiScreen(null);
                        Minecraft.getMinecraft().setIngameFocus();
                        super.mouseReleased(mouseX, mouseY);
                    }
                });
            }
        });
        //stop everything until the gui is closed
        while (Minecraft.getMinecraft().currentScreen != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

