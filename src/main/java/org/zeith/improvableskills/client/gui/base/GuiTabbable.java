package org.zeith.improvableskills.client.gui.base;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.zeith.hammerlib.client.utils.*;
import org.zeith.hammerlib.util.java.tuples.Tuple2;
import org.zeith.hammerlib.util.shaded.json.JSONObject;
import org.zeith.improvableskills.ImprovableSkills;
import org.zeith.improvableskills.SyncSkills;
import org.zeith.improvableskills.api.registry.PageletBase;
import org.zeith.improvableskills.client.gui.GuiCentered;
import org.zeith.improvableskills.client.rendering.ote.OTEConfetti;
import org.zeith.improvableskills.client.rendering.ote.OTETooltip;
import org.zeith.improvableskills.custom.pagelets.PageletUpdate;
import org.zeith.improvableskills.init.PageletsIS;
import org.zeith.improvableskills.init.SoundsIS;
import org.zeith.improvableskills.utils.ScaledResolution;
import org.zeith.improvableskills.utils.Sys;

import java.util.*;

public class GuiTabbable<P extends PageletBase>
		extends GuiCentered
{
	public static final ResourceLocation ICONS = ImprovableSkills.id("textures/gui/icons.png");
	
	public static PageletBase lastPagelet = PageletsIS.SKILLS;
	public static final Map<ResourceLocation, Tuple2.Mutable2<Float, Float>> EXTENSIONS = new HashMap<>();
	
	public final P pagelet;
	protected PageletBase selPgl;
	public Screen parent;
	public final UV gui1, gui2;
	
	List<Component> pageletTooltip = new ArrayList<>();
	
	protected int liveAnimationTime;
	protected boolean zeithBDay = false;
	
	protected final List<PageletBase> pagelets;
	
	public GuiTabbable(P pagelet)
	{
		this.pagelet = pagelet;
		
		lastPagelet = pagelet;
		
		pagelets = new ArrayList<>(ImprovableSkills.PAGELETS.stream().toList());
		pagelets.sort(Comparator.comparing(PageletBase::getRegistryName));
		
		xSize = 195;
		ySize = 168;
		
		gui1 = new UV(ImprovableSkills.id("textures/gui/skills_gui_paper.png"), 0, 0, xSize, ySize);
		gui2 = new UV(ImprovableSkills.id("textures/gui/skills_gui_overlay.png"), 0, 0, xSize, ySize);
	}
	
	protected void drawBack(GuiGraphics pose, float partialTicks, int mouseX, int mouseY)
	{
	}
	
	public void bindIcons()
	{
		FXUtils.bindTexture(ICONS);
	}
	
	int mouseX, mouseY;
	
	@Override
	public void tick()
	{
		super.tick();
		
		final ScaledResolution sr = new ScaledResolution(this.minecraft);
		int i1 = sr.getScaledWidth();
		int j1 = sr.getScaledHeight();
		
		zeithBDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 10 && Calendar.getInstance().get(Calendar.MONTH) == Calendar.NOVEMBER;
		
		if(zeithBDay)
		{
			int[] colors = {
					0xFF_FF0000,
					0xFF_FF6600,
					0xFF_FFFF00,
					0xFF_00FF00,
					0xFF_0000FF,
					0xFF_FF00FF
			};
			int color = colors[colors.length - 1 - (int) ((System.currentTimeMillis() % (colors.length * 3000L)) / 3000L) % colors.length];
			
			if(mouseX > width / 2 - 16 && mouseY > guiTop - 36 && mouseX < width / 2 + 16 && mouseY < guiTop - 4)
				for(int i = 0; i < 4; ++i)
				{
					OTEConfetti cft = new OTEConfetti(width / 2, guiTop - 36 + OTEConfetti.random.nextFloat() * 32);
					cft.motionY = -1.25F;
					cft.motionX = (OTEConfetti.random.nextFloat() - OTEConfetti.random.nextFloat()) * 6F;
					cft.color = color;
				}
		}
	}
	
	protected void setWhiteColor(GuiGraphics gfx)
	{
		gfx.setColor(1F, 1F, 1F, 1F);
	}
	
	protected void setBlueColor(GuiGraphics gfx)
	{
		gfx.setColor(0F, 136 / 255F, 1F, 1F);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY)
	{
		partialTicks = minecraft.getTimer().getRealtimeDeltaTicks(); // hardware acceleration LOL
		var pose = gfx.pose();
		
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		
		renderTransparentBackground(gfx);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		RenderSystem.enableDepthTest();
		
		RenderSystem.enableBlend();
		
		AbstractTexture zeith;
		
		if(zeithBDay && (zeith = GuiCustomButton.getZeithAvatar()) != null)
		{
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			zeith.bind();
			
			pose.pushPose();
			pose.translate(width / 2 - 16, guiTop - 36, 350);
			pose.translate(16, 16, 0);
			pose.mulPose(Axis.ZP.rotationDegrees(6 * OTEConfetti.sineF(System.currentTimeMillis() % 4000L / 1000F)));
			pose.translate(-16, -16, 0);
			RenderUtils.drawFullTexturedModalRect(gfx, 0, 0, 32, 32);
			pose.popPose();
		}
		
		if(PageletUpdate.liveURL != null && (zeith = GuiCustomButton.getZeithAvatar()) != null)
		{
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			zeith.bind();
			
			float s = 16F / font.lineHeight;
			float w = s * font.width("LIVE");
			
			boolean hover = mouseX >= (width - (w + 64)) / 2 && mouseX < (width - (w + 64)) / 2 + w + 64 && mouseY >= guiTop - 36 && mouseY < guiTop - 4;
			
			RenderSystem.setShaderColor(1, 1, 1, 1);
			pose.pushPose();
			{
				pose.translate((width - (w + 64)) / 2, guiTop - 36, 350);
				RenderUtils.drawFullTexturedModalRect(gfx, 0, 0, 32, 32);
				
				pose.pushPose();
				{
					pose.translate(32, 4, 0);
					pose.scale(s, s, 1F);
					gfx.drawString(font, "LIVE", 0, 3, hover ? 0xFFAAAA : 0xFFFFFF, false);
					RenderSystem.setShaderColor(1, 1, 1, 1);
				}
				pose.popPose();
				
				FXUtils.bindTexture("minecraft", "textures/gui/stream_indicator.png");
				
				pose.pushPose();
				{
					pose.translate(w + 32, 0, 0);
					pose.scale(1 / 4F, 1, 1F);
					pose.scale(32 / 60F, 32 / 60F, 1);
					RenderUtils.drawTexturedModalRect(pose, 0, 0, 0, 0, 240, 60);
				}
				pose.popPose();
			}
			pose.popPose();
		}
		
		selPgl = null;
		
		// RIGHT SECTION
		
		int i = 0;
		for(int j = 0; j < pagelets.size(); ++j)
		{
			PageletBase let = pagelets.get(j);
			
			if(!let.isVisible(SyncSkills.getData()) || !let.isRight())
				continue;
			
			boolean mouseOver = mouseX >= guiLeft + 195 && mouseY >= guiTop + 10 + i * 25 && mouseX < guiLeft + 193 + 20 && mouseY < guiTop + 10 + i * 25 + 24;
			
			if(mouseOver)
				selPgl = let;
			
			mouseOver |= pagelet == let;
			
			gui2.bindTexture();
			
			Tuple2.Mutable2<Float, Float> t = EXTENSIONS.get(let.getRegistryName());
			if(t == null) EXTENSIONS.put(let.getRegistryName(), t = new Tuple2.Mutable2<>(0F, 0F));
			t.setA(mouseOver ? 1F : 0F);
			
			float progress = 5 * t.b();
			float dif = Math.max(-.125F, Math.min(.125F, t.a() - t.b()));
			progress += dif * partialTicks;
			progress = (float) (Math.sin(Math.toRadians(progress / 5D * 90)) * 5D);
			if(progress >= 5F && let == pagelet)
			{
				++i;
				continue;
			}
			
			pose.pushPose();
			
			setBlueColor(gfx);
			pose.translate(
					guiLeft + 193 - 7 * ((5 - progress) / 5),
					guiTop + 10 + i * 25,
					0
			);
			
			RenderUtils.drawTexturedModalRect(pose, 0, 0, 236, 0, 20, 24);
			
			pose.translate(0, 0, -50);
			Object icon = let.getIcon();
			
			if(icon instanceof ItemStack stack)
				RenderUtils.renderItemIntoGui(pose, stack, 2, 4);
			else if(icon instanceof AbstractTexture tex)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				tex.bind();
				RenderSystem.setShaderTexture(0, tex.getId());
				RenderUtils.drawFullTexturedModalRect(gfx, 2, 4, 16, 16);
			} else if(icon instanceof UV uv)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				uv.render(pose, 2, 4, 16, 16);
			}
			
			pose.popPose();
			++i;
		}
		
		// LEFT SECTION
		
		i = 0;
		for(int j = 0; j < pagelets.size(); ++j)
		{
			PageletBase let = pagelets.get(j);
			
			if(!let.isVisible(SyncSkills.getData()) || let.isRight())
				continue;
			
			boolean mouseOver = mouseX >= guiLeft - 17 && mouseY >= guiTop + 10 + i * 25 && mouseX < guiLeft && mouseY < guiTop + 10 + i * 25 + 24;
			
			if(mouseOver)
				selPgl = let;
			
			mouseOver |= pagelet == let;
			
			gui2.bindTexture();
			
			Tuple2.Mutable2<Float, Float> t = EXTENSIONS.get(let.getRegistryName());
			if(t == null) EXTENSIONS.put(let.getRegistryName(), t = new Tuple2.Mutable2<>(0F, 0F));
			t.setA(mouseOver ? 1F : 0F);
			
			float progress = 5 * t.b();
			float dif = Math.max(-.125F, Math.min(.125F, t.a() - t.b()));
			progress += dif * partialTicks;
			progress = (float) (Math.sin(Math.toRadians(progress / 5D * 90)) * 5D);
			if(progress >= 5F && let == pagelet)
			{
				++i;
				continue;
			}
			
			pose.pushPose();
			setBlueColor(gfx);
			pose.translate(guiLeft - 18 + 7 * ((5 - progress) / 5), guiTop + 10 + i * 25, 0);
			pose.pushPose();
			pose.translate(10, 14, 0);
			pose.scale(-1, -1, 1);
			pose.translate(-10, -14, 0);
			RenderUtils.drawTexturedModalRect(pose, 0, 4, 236, 0, 20, 24);
			pose.popPose();
			pose.translate(0, 0, -50);
			Object icon = let.getIcon();
			if(icon instanceof ItemStack stack)
				RenderUtils.renderItemIntoGui(pose, stack, 2, 4);
			if(icon instanceof AbstractTexture tex)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				tex.bind();
				RenderSystem.setShaderTexture(0, tex.getId());
				RenderUtils.drawFullTexturedModalRect(gfx, 2, 4, 16, 16);
			} else if(icon instanceof UV uv)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				uv.render(pose, 0, 0, 16, 16);
			}
			pose.popPose();
			++i;
		}
		
		//
		
		setWhiteColor(gfx);
		
		pose.pushPose();
		pose.translate(0, 0, 100);
		drawBack(gfx, partialTicks, mouseX, mouseY);
		pose.popPose();
		
		//
		
		if(selPgl != null)
		{
			pageletTooltip.clear();
			
			selPgl.addTitle(pageletTooltip);
			
			OTETooltip.showTooltip(pageletTooltip);
		}
		
		//
		
		float s = 16F / font.lineHeight;
		float w = s * font.width("LIVE");
		
		if(PageletUpdate.liveURL != null && mouseX >= (width - (w + 64)) / 2 && mouseX < (width - (w + 64)) / 2 + w + 64 && mouseY >= guiTop - 36 && mouseY < guiTop - 4)
		{
			OTETooltip.showTooltip(
					Component.literal("Zeitheron is LIVE!"),
					Component.literal(JSONObject.quote(PageletUpdate.liveTitle)),
					Component.literal("Click to watch!")
			);
		} else if(zeithBDay && mouseX > width / 2 - 16 && mouseY > guiTop - 36 && mouseX < width / 2 + 16 && mouseY < guiTop - 4)
		{
			OTETooltip.showTooltip(Component.literal("Happy birthday, Zeitheron!"));
		}
		
		// RIGHT SELECTOR
		
		i = 0;
		for(int j = 0; j < pagelets.size(); ++j)
		{
			PageletBase let = pagelets.get(j);
			if(!let.isVisible(SyncSkills.getData()) || !let.isRight()) continue;
			boolean mouseOver = mouseX >= guiLeft + 195 && mouseY >= guiTop + 10 + i * 25 && mouseX < guiLeft + 193 + 20 && mouseY < guiTop + 10 + i * 25 + 24;
			if(mouseOver) selPgl = let;
			mouseOver |= pagelet == let;
			gui2.bindTexture();
			Tuple2.Mutable2<Float, Float> t = EXTENSIONS.get(let.getRegistryName());
			if(t == null) EXTENSIONS.put(let.getRegistryName(), t = new Tuple2.Mutable2<>(0F, 0F));
			t.setA(mouseOver ? 1F : 0F);
			float progress = 5 * t.b();
			float dif = Math.max(-.125F, Math.min(.125F, t.a() - t.b()));
			progress += dif * partialTicks;
			progress = (float) (Math.sin(Math.toRadians(progress / 5D * 90)) * 5D);
			if(progress < 5 || let != pagelet)
			{
				++i;
				continue;
			}
			pose.pushPose();
			setBlueColor(gfx);
			pose.translate(
					guiLeft + 193 - 7 * ((5 - progress) / 5),
					guiTop + 10 + i * 25,
					200
			);
			RenderUtils.drawTexturedModalRect(pose, 0, 0, 236, 0, 20, 24);
			pose.translate(0, 0, -50);
			Object icon = let.getIcon();
			if(icon instanceof ItemStack stack)
				RenderUtils.renderItemIntoGui(pose, stack, 2, 4);
			else if(icon instanceof AbstractTexture tex)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				tex.bind();
				RenderSystem.setShaderTexture(0, tex.getId());
				RenderUtils.drawFullTexturedModalRect(gfx, 2, 4, 16, 16);
			} else if(icon instanceof UV uv)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				uv.render(pose, 2, 4, 16, 16);
			}
			pose.popPose();
			++i;
		}
		
		// LEFT SELECTOR
		
		i = 0;
		for(int j = 0; j < pagelets.size(); ++j)
		{
			PageletBase let = pagelets.get(j);
			if(!let.isVisible(SyncSkills.getData()) || let.isRight()) continue;
			boolean mouseOver = mouseX >= guiLeft - 17 && mouseY >= guiTop + 10 + i * 25 && mouseX < guiLeft && mouseY < guiTop + 10 + i * 25 + 24;
			if(mouseOver) selPgl = let;
			mouseOver |= pagelet == let;
			gui2.bindTexture();
			Tuple2.Mutable2<Float, Float> t = EXTENSIONS.get(let.getRegistryName());
			if(t == null) EXTENSIONS.put(let.getRegistryName(), t = new Tuple2.Mutable2<>(0F, 0F));
			t.setA(mouseOver ? 1F : 0F);
			float progress = 5 * t.b();
			float dif = Math.max(-.125F, Math.min(.125F, t.a() - t.b()));
			progress += dif * partialTicks;
			progress = (float) (Math.sin(Math.toRadians(progress / 5D * 90)) * 5D);
			if(progress < 5 || let != pagelet)
			{
				++i;
				continue;
			}
			pose.pushPose();
			setBlueColor(gfx);
			pose.translate(
					guiLeft - 18 + 7 * ((5 - progress) / 5),
					guiTop + 10 + i * 25,
					200
			);
			pose.pushPose();
			pose.translate(10, 14, 0);
			pose.scale(-1, -1, 1);
			pose.translate(-10, -14, 0);
			RenderUtils.drawTexturedModalRect(pose, 0, 4, 236, 0, 20, 24);
			pose.popPose();
			pose.translate(0, 0, -50);
			Object icon = let.getIcon();
			if(icon instanceof ItemStack stack)
				RenderUtils.renderItemIntoGui(pose, stack, 2, 4);
			if(icon instanceof AbstractTexture tex)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				tex.bind();
				RenderSystem.setShaderTexture(0, tex.getId());
				RenderUtils.drawFullTexturedModalRect(gfx, 2, 4, 16, 16);
			} else if(icon instanceof UV uv)
			{
				pose.translate(0, 0, 150);
				setWhiteColor(gfx);
				uv.render(pose, 0, 0, 16, 16);
			}
			pose.popPose();
			++i;
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		if(selPgl != null)
		{
			if(selPgl.hasTab())
			{
				if(pagelet != selPgl)
					minecraft.setScreen(selPgl.createTab(SyncSkills.getData()));
			} else
				selPgl.onClick();
			
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundsIS.PAGE_TURNS, 1F));
			
			return true;
		}
		
		float s = 16F / font.lineHeight;
		float w = s * font.width("LIVE");
		
		if(PageletUpdate.liveURL != null && mouseX >= (width - (w + 64)) / 2 && mouseX < (width - (w + 64)) / 2 + w + 64 && mouseY >= guiTop - 36 && mouseY < guiTop - 4)
		{
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
			Sys.openURL(PageletUpdate.liveURL);
			return true;
		}
		
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
}