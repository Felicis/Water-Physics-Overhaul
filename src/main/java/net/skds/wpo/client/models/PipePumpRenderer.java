package net.skds.wpo.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.skds.wpo.WPO;
import net.skds.wpo.block.tileentity.PipePumpTileEntity;

@OnlyIn(Dist.CLIENT)
public class PipePumpRenderer extends TileEntityRenderer<PipePumpTileEntity> {

	private final ModelRenderer zasos;
	private final ModelRenderer cube_r1, cube_r2, cube_r3, cube_r4;
	private final ModelRenderer terebilka;

	private static final ResourceLocation TEXTURE = new ResourceLocation(WPO.MOD_ID, "textures/block/pump_te2.png");
	private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE);

	public PipePumpRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
		super(rendererDispatcherIn);
		// rendererDispatcherIn.textureManager.bindTexture(TEXTURE);

		zasos = new ModelRenderer(64, 64, 0, 0);
		zasos.setPos(8.0F, 8.0F, -8.0F);
		zasos.texOffs(0, 0).addBox(-13.0F, -13.0F, 0.0F, 10.0F, 10.0F, 16.0F, 0.0F, false);
		zasos.texOffs(0, 46).addBox(-1.0F, -11.0F, 2.0F, 1.0F, 6.0F, 12.0F, 0.0F, false);
		zasos.texOffs(0, 46).addBox(-16.0F, -11.0F, 2.0F, 1.0F, 6.0F, 12.0F, 0.0F, true);
		zasos.texOffs(26, 47).addBox(-11.0F, -16.0F, 14.0F, 6.0F, 16.0F, 1.0F, 0.0F, false);

		cube_r2 = new ModelRenderer(64, 64, 0, 0);
		cube_r2.setPos(-8.0F, -8.0F, 1.5F);
		zasos.addChild(cube_r2);
		setRotationAngle(cube_r2, 3.1416F, 0.0F, -1.5708F);
		cube_r2.texOffs(26, 47).addBox(-3.0F, -8.0F, -0.5F, 6.0F, 16.0F, 1.0F, 0.0F, false);

		cube_r3 = new ModelRenderer(64, 64, 0, 0);
		cube_r3.setPos(-8.0F, -8.0F, 14.5F);
		zasos.addChild(cube_r3);
		setRotationAngle(cube_r3, 0.0F, 0.0F, -1.5708F);
		cube_r3.texOffs(26, 47).addBox(-3.0F, -8.0F, -0.5F, 6.0F, 16.0F, 1.0F, 0.0F, false);

		cube_r4 = new ModelRenderer(64, 64, 0, 0);
		cube_r4.setPos(-8.0F, -8.0F, 1.5F);
		zasos.addChild(cube_r4);
		setRotationAngle(cube_r4, 3.1416F, 0.0F, 0.0F);
		cube_r4.texOffs(26, 47).addBox(-3.0F, -8.0F, -0.5F, 6.0F, 16.0F, 1.0F, 0.0F, false);

		cube_r1 = new ModelRenderer(64, 64, 0, 0);
		cube_r1.setPos(-8.0F, -8.0F, 8.0F);
		zasos.addChild(cube_r1);
		setRotationAngle(cube_r1, 0.0F, 0.0F, 1.5708F);
		cube_r1.texOffs(0, 46).addBox(-8.0F, -3.0F, -6.0F, 1.0F, 6.0F, 12.0F, 0.0F, true);
		cube_r1.texOffs(0, 46).addBox(7.0F, -3.0F, -6.0F, 1.0F, 6.0F, 12.0F, 0.0F, false);

		terebilka = new ModelRenderer(64, 64, 0, 0);
		terebilka.setPos(8.0F, 8.0F, -8.0F);
		terebilka.texOffs(0, 26).addBox(-15.0F, -15.0F, 12.0F, 14.0F, 14.0F, 2.0F, 0.0F, false);

		
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}

	
	@Override
	public void render(PipePumpTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn,
			IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
		// RenderMaterial m = new RenderMaterial(ATLAS, TEXTURE);
		// IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getCutout());
		IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RENDER_TYPE);
		matrixStackIn.pushPose();
		matrixStackIn.translate(0.5, 0.5, 0.5);

		Direction dir = tileEntityIn.facing;
		switch (dir) {
			case UP:
				matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(90));
				break;
			case DOWN:
				matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-90));
				break;
			case EAST:
				matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(-90));
				break;
			case NORTH:
				break;
			case SOUTH:
				matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180));
				break;
			case WEST:
				matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(90));
				break;
			default:
				break;
		}

		zasos.render(matrixStackIn, ivertexbuilder, combinedLightIn, combinedOverlayIn);

		float partial = partialTicks;
		if (tileEntityIn.powered) {

		} else {
			if (tileEntityIn.anim >= 0) {
				partial = 1 - partial;
			} else {
				partial = 0;
			}
		}

		float anim = tileEntityIn.anim + partial;
		if (tileEntityIn.anim < 0) {
			anim ++;
		}

		float phase = (float) Math.PI * anim / (tileEntityIn.animSpeed + 1);

		//System.out.println(anim);
		//System.out.println(Math.abs(Math.sin(phase)));
		
		float offset = -0.625F + (float) Math.abs(Math.cos(phase)) * 0.625F;
		//float offset = -0.4F;

		matrixStackIn.translate(0, 0, offset);

		terebilka.render(matrixStackIn, ivertexbuilder, combinedLightIn, combinedOverlayIn);

		matrixStackIn.popPose();
	}
}