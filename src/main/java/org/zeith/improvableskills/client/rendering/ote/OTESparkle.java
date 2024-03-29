package org.zeith.improvableskills.client.rendering.ote;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.zeith.hammerlib.client.utils.FXUtils;
import org.zeith.hammerlib.client.utils.RenderUtils;
import org.zeith.hammerlib.util.colors.ColorHelper;
import org.zeith.improvableskills.ImprovableSkills;
import org.zeith.improvableskills.client.rendering.OTEffect;
import org.zeith.improvableskills.utils.ScaledResolution;
import org.zeith.improvableskills.utils.Trajectory;

public class OTESparkle
		extends OTEffect
{
	private int color;
	private double tx, ty;
	private int totTime, prevTime, time;
	public double[] xPoints, yPoints;
	
	Class<? extends Screen> screen;
	
	public OTESparkle(double x, double y, double tx, double ty, int time, int color)
	{
		renderHud = false;
		this.totTime = time;
		this.x = this.prevX = x;
		this.y = this.prevY = y;
		this.tx = tx;
		this.ty = ty;
		this.color = color;
		
		double[][] path = Trajectory.makeBroken2DTrajectory(x, y, tx, ty, time, Math.abs(hashCode() / 25F));
		xPoints = path[0];
		yPoints = path[1];
		
		screen = Minecraft.getInstance().screen != null ? Minecraft.getInstance().screen.getClass() : null;
	}
	
	@Override
	public void resize(ScaledResolution prev, ScaledResolution nev)
	{
		super.resize(prev, nev);
		tx = handleResizeXd(tx, prev, nev);
		ty = handleResizeYd(ty, prev, nev);
		xPoints = handleResizeXdv(xPoints, prev, nev);
		yPoints = handleResizeYdv(yPoints, prev, nev);
	}
	
	@Override
	public void update()
	{
		super.update();
		prevTime = time;
		
		int tt = xPoints.length;
		
		int cframe = (int) Math.round(time / (float) totTime * tt);
		
		x = xPoints[cframe];
		y = yPoints[cframe];
		
		time++;
		
		if(time >= totTime)
			setExpired();
	}
	
	@Override
	public void render(GuiGraphics gfx, float partialTime)
	{
		var pose = gfx.pose();
		Screen gui = Minecraft.getInstance().screen;
		if((gui == null && screen == null) || (screen != null && screen.isInstance(gui)))
		{
			double cx = prevX + (x - prevX) * partialTime;
			double cy = prevY + (y - prevY) * partialTime;
			float t = prevTime + partialTime;
			float r = (float) (System.currentTimeMillis() % 2000L) / 2000.0F;
			r = r > 0.5F ? 1.0F - r : r;
			r += 0.45F;
			
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			FXUtils.bindTexture(ImprovableSkills.MOD_ID, "textures/particles/sparkle.png");
			
			int tx = 64 * (int) (time / (float) totTime * 3F);
			
			float scale = 1 / 8F;
			
			if(t < 5)
				scale *= t / 5F;
			
			if(t >= totTime - 5)
				scale *= 1 - (t - totTime + 5) / 5F;
			
			RenderSystem.setShaderColor(ColorHelper.getRed(color), ColorHelper.getGreen(color), ColorHelper.getBlue(color), .9F * ColorHelper.getAlpha(color));
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(770, 772);
			
			for(int i = 0; i < 4; ++i)
			{
				float ps = scale / (i + 1F);
				
				pose.pushPose();
				pose.translate(cx - 64 * ps / 2, cy - 64 * ps / 2, 5);
				pose.scale(ps, ps, ps);
				RenderUtils.drawTexturedModalRect(pose, 0, 0, tx, 0, 64, 64);
				pose.popPose();
			}
			
			RenderSystem.defaultBlendFunc();
			setWhiteColor();
		}
	}
}