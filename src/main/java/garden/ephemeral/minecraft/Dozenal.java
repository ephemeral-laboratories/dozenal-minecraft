package garden.ephemeral.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Dozenal.MODID, name = Dozenal.NAME, version = Dozenal.VERSION)
public class Dozenal
{
    public static final String MODID = "garden.ephemeral.dozenal";
    public static final String NAME = "Dozenal";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        Minecraft minecraft = Minecraft.getMinecraft();

        // Mimics what's going on in Minecraft class.
        FontRenderer fontRenderer = new DozenalFontRenderer(
                minecraft.gameSettings,
                new ResourceLocation("textures/font/ascii.png"),
                new ResourceLocation("textures/font/dozenal.png"),
                minecraft.renderEngine,
                minecraft.isUnicode());
        if (minecraft.gameSettings.language != null) {
            fontRenderer.setUnicodeFlag(minecraft.isUnicode());
            minecraft.getLanguageManager().isCurrentLanguageBidirectional();
        }
        IResourceManager resourceManager = minecraft.getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resourceManager).registerReloadListener(fontRenderer);
        }
        minecraft.fontRenderer = fontRenderer;
    }
}
