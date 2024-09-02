package org.zeith.improvableskills.client.gui.abil.ench;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.zeith.hammerlib.util.colors.ColorHelper;
import org.zeith.hammerlib.util.mcf.Resources;
import org.zeith.improvableskills.ImprovableSkills;
import org.zeith.improvableskills.SyncSkills;
import org.zeith.improvableskills.client.rendering.OnTopEffects;
import org.zeith.improvableskills.client.rendering.ote.OTESparkle;

import java.util.List;
import java.util.Optional;

import static org.zeith.improvableskills.client.gui.abil.ench.GuiEnchPowBook.DEFAULT_GLINT_COLOR;

public class GuiPortableEnchantment
		extends AbstractContainerScreen<ContainerPortableEnchantment>
{
	private static final ResourceLocation ENCHANTING_TABLE_LOCATION = Resources.location("textures/gui/container/enchanting_table.png");
	private static final ResourceLocation ENCHANTMENT_TABLE_BOOK_TEXTURE1 = ImprovableSkills.id("textures/gui/enchanting_table_book_1.png");
	private static final ResourceLocation ENCHANTMENT_TABLE_BOOK_TEXTURE2 = ImprovableSkills.id("textures/gui/enchanting_table_book_2.png");
	
	private static final ResourceLocation[] ENABLED_LEVEL_SPRITES = new ResourceLocation[] {
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1"),
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2"),
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3")
	};
	private static final ResourceLocation[] DISABLED_LEVEL_SPRITES = new ResourceLocation[] {
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1_disabled"),
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2_disabled"),
			ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3_disabled")
	};
	private static final ResourceLocation ENCHANTMENT_SLOT_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace(
			"container/enchanting_table/enchantment_slot_disabled"
	);
	private static final ResourceLocation ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace(
			"container/enchanting_table/enchantment_slot_highlighted"
	);
	private static final ResourceLocation ENCHANTMENT_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/enchanting_table/enchantment_slot");
	
	
	private final RandomSource random = RandomSource.create();
	private BookModel bookModel;
	public int time;
	public float flip;
	public float oFlip;
	public float flipT;
	public float flipA;
	public float open;
	public float oOpen;
	private ItemStack last = ItemStack.EMPTY;
	
	public GuiPortableEnchantment(ContainerPortableEnchantment container, Inventory playerInv, Component label)
	{
		super(container, playerInv, label);
	}
	
	@Override
	protected void init()
	{
		super.init();
		this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
	}
	
	@Override
	public void containerTick()
	{
		super.containerTick();
		this.tickBook();
		
		for(int section = 0; section < 3; ++section)
			if((this.menu).costs[section] > 0 && random.nextInt(6) == 0)
			{
				String ln = I18n.get("text.improvableskills:enchpower", (int) SyncSkills.getData().enchantPower);
				
				float w = random.nextFloat();
				
				float x1 = leftPos + 60 + (108 - this.font.width(ln)) / 2 + w * this.font.width(ln);
				float y1 = topPos + 3 + this.font.lineHeight;
				
				float x2 = leftPos + 60 + w * 108;
				float y2 = topPos + 14 + 19 * section + random.nextFloat() * 19;
				
				OnTopEffects.effects.add(new OTESparkle(x1, y1, x2, y2, 50 + random.nextInt(30), DEFAULT_GLINT_COLOR));
			}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		int i = (this.width - this.imageWidth) / 2;
		int j = (this.height - this.imageHeight) / 2;
		
		if(mouseButton == 0)
		{
			mouseX -= leftPos;
			mouseY -= topPos;
			
			String ln = I18n.get("text.improvableskills:enchpower", (int) SyncSkills.getData().enchantPower);
			boolean mouseOverChant = mouseX >= 60 + (108 - font.width(ln)) / 2 && mouseY > 3 && mouseX < 60 + (108 - font.width(ln)) / 2 + font.width(ln) && mouseY < 3 + font.lineHeight;
			if(mouseOverChant)
			{
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, .7F + random.nextFloat() * .1F));
				this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, 122);
				return true;
			}
			
			mouseX += leftPos;
			mouseY += topPos;
		}
		
		for(int k = 0; k < 3; ++k)
		{
			double d0 = mouseX - (double) (i + 60);
			double d1 = mouseY - (double) (j + 14 + 19 * k);
			if(d0 >= 0.0D && d1 >= 0.0D && d0 < 108.0D && d1 < 19.0D && this.menu.clickMenuButton(this.minecraft.player, k))
			{
				this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, k);
				
				for(int m = 0; m < 10; ++m)
				{
					float x1 = i + 23 + random.nextFloat() * 22;
					float y1 = j + 23 + random.nextFloat() * 12;
					
					float x2 = i + menu.slots.get(0).x + random.nextFloat() * 16;
					float y2 = j + menu.slots.get(0).y + random.nextFloat() * 16;
					
					OnTopEffects.effects.add(new OTESparkle(x1, y1, x2, y2, 40 - random.nextInt(30), 0xFF0087FF));
				}
				
				return true;
			}
		}
		
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY)
	{
		mouseX -= leftPos;
		mouseY -= topPos;
		
		var ln = Component.literal(I18n.get("text.improvableskills:enchpower", (int) SyncSkills.getData().enchantPower)).withStyle(ChatFormatting.UNDERLINE);
		boolean mouseOverChant = mouseX >= 60 + (108 - font.width(ln)) / 2 && mouseY > 3 && mouseX < 60 + (108 - font.width(ln)) / 2 + font.width(ln) && mouseY < 3 + font.lineHeight;
		if(mouseOverChant)
			ln = ln.withStyle(ChatFormatting.BLUE);
		gfx.drawString(font, ln, 60 + (108 - font.width(ln)) / 2, 3, 4210752, false);
		
		super.renderLabels(gfx, mouseX, mouseY);
	}
	
	@Override
	protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY)
	{
		Lighting.setupForFlatItems();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		gfx.blit(ENCHANTING_TABLE_LOCATION, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
		
		renderBook(gfx, leftPos, topPos, partial);
		
		EnchantmentNames.getInstance().initSeed(this.menu.getEnchantmentSeed());
		int k = this.menu.getGoldCount();
		
		for(int l = 0; l < 3; ++l)
		{
			int i1 = leftPos + 60;
			int j1 = i1 + 20;
			int k1 = (this.menu).costs[l];
			if(k1 == 0)
			{
				RenderSystem.enableBlend();
				gfx.blitSprite(ENCHANTMENT_SLOT_DISABLED_SPRITE, i1, topPos + 14 + 19 * l, 108, 19);
				RenderSystem.disableBlend();
			} else
			{
				String s = "" + k1;
				int l1 = 86 - this.font.width(s);
				FormattedText formattedtext = EnchantmentNames.getInstance().getRandomName(this.font, l1);
				int i2 = 6839882;
				if(((k < l + 1 || this.minecraft.player.experienceLevel < k1) && !this.minecraft.player.getAbilities().instabuild) || this.menu.enchantClue[l] == -1)
				{
					RenderSystem.enableBlend();
					gfx.blitSprite(ENCHANTMENT_SLOT_DISABLED_SPRITE, i1, topPos + +14 + 19 * l, 108, 19);
					gfx.blitSprite(DISABLED_LEVEL_SPRITES[l], i1 + 1, topPos + +15 + 19 * l, 16, 16);
					RenderSystem.disableBlend();
					gfx.drawWordWrap(this.font, formattedtext, j1, topPos + 16 + 19 * l, l1, (i2 & 16711422) >> 1);
					i2 = 4226832;
				} else
				{
					int j2 = mouseX - (leftPos + 60);
					int k2 = mouseY - (topPos + 14 + 19 * l);
					if(j2 >= 0 && k2 >= 0 && j2 < 108 && k2 < 19)
					{
						gfx.blitSprite(ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE, i1, topPos + 14 + 19 * l, 108, 19);
						i2 = 16777088;
					} else
					{
						gfx.blitSprite(ENCHANTMENT_SLOT_SPRITE, i1, topPos + 14 + 19 * l, 108, 19);
					}
					
					gfx.blitSprite(ENABLED_LEVEL_SPRITES[l], i1 + 1, topPos + 15 + 19 * l, 16, 16);
					RenderSystem.disableBlend();
					gfx.drawWordWrap(this.font, formattedtext, j1, topPos + 16 + 19 * l, l1, i2);
					i2 = 8453920;
				}
				
				gfx.drawString(this.font, s, j1 + 86 - this.font.width(s), topPos + 16 + 19 * l + 7, i2);
			}
		}
	}
	
	private void renderBook(GuiGraphics gfx, int x, int y, float partial)
	{
		var pose = gfx.pose();
		
		float f = Mth.lerp(partial, this.oOpen, this.open);
		float f1 = Mth.lerp(partial, this.oFlip, this.flip);
		Lighting.setupForEntityInInventory();
		
		pose.pushPose();
		pose.translate((float) x + 33.0F, (float) y + 31.0F, 100.0F);
		pose.scale(-40.0F, 40.0F, 40.0F);
		pose.mulPose(Axis.XP.rotationDegrees(25.0F));
		pose.translate((1.0F - f) * 0.2F, (1.0F - f) * 0.1F, (1.0F - f) * 0.25F);
		
		pose.mulPose(Axis.YP.rotationDegrees(-(1.0F - f) * 90.0F - 90.0F));
		pose.mulPose(Axis.XP.rotationDegrees(180.0F));
		
		float f4 = Mth.clamp(Mth.frac(f1 + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
		float f5 = Mth.clamp(Mth.frac(f1 + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
		this.bookModel.setupAnim(0.0F, f4, f5, f);
		
		var vrtx = gfx.bufferSource().getBuffer(RenderType.entityCutout(ENCHANTMENT_TABLE_BOOK_TEXTURE1));
		this.bookModel.renderToBuffer(pose, vrtx, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ColorHelper.packARGB(1.0F, 0F, 136 / 255F, 1F));
		
		vrtx = gfx.bufferSource().getBuffer(RenderType.entityCutout(ENCHANTMENT_TABLE_BOOK_TEXTURE2));
		this.bookModel.renderToBuffer(pose, vrtx, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ColorHelper.packARGB(1.0F, 1.0F, 1.0F, 1.0F));
		
		gfx.flush();
		
		pose.popPose();
		Lighting.setupFor3DItems();
	}
	
	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial)
	{
		partial = this.minecraft.getTimer().getRealtimeDeltaTicks();
		renderTransparentBackground(gfx);
		super.render(gfx, mouseX, mouseY, partial);
		this.renderTooltip(gfx, mouseX, mouseY);
		boolean flag = this.minecraft.player.getAbilities().instabuild;
		int i = this.menu.getGoldCount();
		
		for(int j = 0; j < 3; ++j)
		{
			int k = (this.menu).costs[j];
			
			Optional<Holder.Reference<Enchantment>> optional = this.minecraft
					.level
					.registryAccess()
					.registryOrThrow(Registries.ENCHANTMENT)
					.getHolder(this.menu.enchantClue[j]);
			
			int l = (this.menu).levelClue[j];
			int i1 = j + 1;
			if(this.isHovering(60, 14 + 19 * j, 108, 17, mouseX, mouseY) && k > 0)
			{
				List<Component> list = Lists.newArrayList();
				list.add(Component.translatable("container.enchant.clue", optional.isEmpty() ? "" : Enchantment.getFullname(optional.get(), l)).withStyle(ChatFormatting.WHITE));
				if(optional.isEmpty())
				{
					list.add(Component.literal(""));
					list.add(Component.translatable("neoforge.container.enchant.limitedEnchantability").withStyle(ChatFormatting.RED));
				} else if(!flag)
				{
					list.add(CommonComponents.EMPTY);
					if(this.minecraft.player.experienceLevel < k)
					{
						list.add(Component.translatable("container.enchant.level.requirement", (this.menu).costs[j]).withStyle(ChatFormatting.RED));
					} else
					{
						MutableComponent mutablecomponent;
						if(i1 == 1)
						{
							mutablecomponent = Component.translatable("container.enchant.lapis.one");
						} else
						{
							mutablecomponent = Component.translatable("container.enchant.lapis.many", i1);
						}
						
						list.add(mutablecomponent.withStyle(i >= i1 ? ChatFormatting.GRAY : ChatFormatting.RED));
						MutableComponent mutablecomponent1;
						if(i1 == 1)
						{
							mutablecomponent1 = Component.translatable("container.enchant.level.one");
						} else
						{
							mutablecomponent1 = Component.translatable("container.enchant.level.many", i1);
						}
						
						list.add(mutablecomponent1.withStyle(ChatFormatting.GRAY));
					}
				}
				
				gfx.renderComponentTooltip(this.font, list, mouseX, mouseY);
				break;
			}
		}
	}
	
	public void tickBook()
	{
		ItemStack itemstack = this.menu.getSlot(0).getItem();
		if(!ItemStack.matches(itemstack, this.last))
		{
			this.last = itemstack;
			
			do
			{
				this.flipT += (float) (this.random.nextInt(4) - this.random.nextInt(4));
			} while(this.flip <= this.flipT + 1.0F && this.flip >= this.flipT - 1.0F);
		}
		
		++this.time;
		this.oFlip = this.flip;
		this.oOpen = this.open;
		boolean flag = false;
		
		for(int i = 0; i < 3; ++i)
		{
			if((this.menu).costs[i] != 0)
			{
				flag = true;
				break;
			}
		}
		
		if(flag)
		{
			this.open += 0.2F;
		} else
		{
			this.open -= 0.2F;
		}
		
		this.open = Mth.clamp(this.open, 0.0F, 1.0F);
		float f1 = (this.flipT - this.flip) * 0.4F;
		float f = 0.2F;
		f1 = Mth.clamp(f1, -0.2F, 0.2F);
		this.flipA += (f1 - this.flipA) * 0.9F;
		this.flip += this.flipA;
	}
}