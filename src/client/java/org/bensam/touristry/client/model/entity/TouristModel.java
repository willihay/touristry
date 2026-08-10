package org.bensam.touristry.client.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import org.bensam.touristry.Touristry;
import net.minecraft.util.Mth;
import org.bensam.touristry.client.render.entity.state.TouristRenderState;

@Environment(EnvType.CLIENT)
public class TouristModel extends EntityModel<TouristRenderState> implements HeadedModel, VillagerLikeModel<TouristRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist"),
            "main"
    );
    public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Touristry.MOD_ID, "tourist"),
            "baby"
    );

    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart crossedArms;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public TouristModel(ModelPart modelPart) {
        super(modelPart);
        this.head = modelPart.getChild("head");
        this.rightLeg = modelPart.getChild("right_leg");
        this.leftLeg = modelPart.getChild("left_leg");
        this.crossedArms = modelPart.getChild("arms");
        this.rightArm = modelPart.getChild("right_arm");
        this.leftArm = modelPart.getChild("left_arm");
    }

    public static MeshDefinition createBodyModel() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        float f = 0.5F;
        PartDefinition partDefinition2 = partDefinition.addOrReplaceChild(
                "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO
        );
        PartDefinition partDefinition3 = partDefinition2.addOrReplaceChild(
                "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO
        );
        partDefinition3.addOrReplaceChild(
                "hat_rim", CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F), PartPose.rotation((float) (-Math.PI / 2), 0.0F, 0.0F)
        );
        partDefinition2.addOrReplaceChild(
                "nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 0.0F)
        );
        PartDefinition partDefinition4 = partDefinition.addOrReplaceChild(
                "body", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO
        );
        partDefinition4.addOrReplaceChild(
                "jacket", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO
        );
        partDefinition.addOrReplaceChild(
                "arms",
                CubeListBuilder.create()
                        .texOffs(44, 22)
                        .addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(44, 22)
                        .addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, true)
                        .texOffs(40, 38)
                        .addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F)
        );
        partDefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(44, 22).addBox(-3.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(44, 38).addBox(-3.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F), // hand region
                PartPose.offset(-5.0F, 1.0F, 0.0F)
        );
        partDefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(44, 22).mirror().addBox(-1.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                        .texOffs(44, 38).mirror().addBox(-1.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F), // hand region
                PartPose.offset(5.0F, 1.0F, 0.0F)
        );
        partDefinition.addOrReplaceChild(
                "right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F)
        );
        partDefinition.addOrReplaceChild(
                "left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F)
        );
        return meshDefinition;
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(createBodyModel(), 64, 64);
    }

    public static LayerDefinition createBabyBodyLayer() {
        return createBodyLayer().apply(BABY_TRANSFORMER);
    }

    public static MeshDefinition createNoHatModel() {
        MeshDefinition meshDefinition = createBodyModel();
        meshDefinition.getRoot().getChild("head").clearChild("hat").clearRecursively();
        return meshDefinition;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    public void setupAnim(TouristRenderState entityRenderState) {
        super.setupAnim(entityRenderState);
        this.head.yRot = entityRenderState.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = entityRenderState.xRot * (float) (Math.PI / 180.0);
        if (entityRenderState.isUnhappy) {
            this.head.zRot = 0.3F * Mth.sin(0.45F * entityRenderState.ageInTicks);
            this.head.xRot = 0.4F;
        } else {
            this.head.zRot = 0.0F;
        }

        this.rightLeg.xRot = Mth.cos(entityRenderState.walkAnimationPos * 0.6662F) * 1.4F * entityRenderState.walkAnimationSpeed * 0.5F;
        this.leftLeg.xRot = Mth.cos(entityRenderState.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * entityRenderState.walkAnimationSpeed * 0.5F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        this.crossedArms.visible = !entityRenderState.isWaving;
        this.rightArm.visible = entityRenderState.isWaving;
        this.leftArm.visible = entityRenderState.isWaving;
        if (entityRenderState.isWaving) {
            float wave = Mth.sin(entityRenderState.ageInTicks * 0.4F);
            this.rightArm.xRot = -2.1F + wave * 0.25F;
            this.rightArm.zRot = -0.2F + wave * 0.4F;
        }
    }

    @Override
    public void translateToArms(TouristRenderState entityRenderState, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.crossedArms.translateAndRotate(poseStack);
    }
}
