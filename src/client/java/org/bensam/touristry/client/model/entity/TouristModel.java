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
    private final ModelPart arms;
    private final ModelPart crossedArms;
    private final ModelPart straightArms;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public TouristModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.arms = root.getChild("arms");
        this.crossedArms = this.arms.getChild("crossed_arms");
        this.straightArms = this.arms.getChild("straight_arms");
        this.rightArm = this.straightArms.getChild("right_arm");
        this.leftArm = this.straightArms.getChild("left_arm");
    }

    public static MeshDefinition createBodyModel() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        PartDefinition head = partDefinition.addOrReplaceChild(
                "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO
        );
        PartDefinition hat = head.addOrReplaceChild(
                "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO
        );
        hat.addOrReplaceChild(
                "hat_rim", CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F), PartPose.rotation((float) (-Math.PI / 2.0), 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
                "nose", CubeListBuilder.create().texOffs(25, 1).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -2.0F, 0.0F)
        );
        PartDefinition body = partDefinition.addOrReplaceChild(
                "body", CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO
        );
        body.addOrReplaceChild(
                "jacket", CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO
        );
        PartDefinition arms = partDefinition.addOrReplaceChild(
                "arms", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 2.95F, -1.05F, -0.1745F, 0.0F, 0.0F));
        PartDefinition crossed_arms = arms.addOrReplaceChild(
                "crossed_arms", CubeListBuilder.create().texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(44, 22).mirror().addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F).mirror(false), PartPose.rotation(-0.576F, 0.0F, 0.0F));
        crossed_arms.addOrReplaceChild(
                "right_hand", CubeListBuilder.create().texOffs(44, 38).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 4.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 2.0)));
        crossed_arms.addOrReplaceChild(
                "left_hand", CubeListBuilder.create().texOffs(44, 38).mirror().addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F).mirror(false), PartPose.offsetAndRotation(2.0F, 4.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2.0)));
        PartDefinition straight_arms = arms.addOrReplaceChild(
                "straight_arms", CubeListBuilder.create(), PartPose.ZERO);
        straight_arms.addOrReplaceChild(
                "right_arm", CubeListBuilder.create().texOffs(44, 22).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(44, 38).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(-6.0F, -1.0F, 0.0F));
        straight_arms.addOrReplaceChild(
                "left_arm", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(-2.0F, -1.0F, -2.0F, 4.0F, 8.0F, 4.0F).mirror(false)
                .texOffs(44, 38).mirror().addBox(-2.0F, 7.0F, -2.0F, 4.0F, 4.0F, 4.0F).mirror(false), PartPose.offset(6.0F, -1.0F, 0.0F));
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

    @Override
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
        this.straightArms.visible = entityRenderState.isWaving;
        if (entityRenderState.isWaving) {
            float wave = Mth.sin(entityRenderState.ageInTicks * 0.4F);
            if (entityRenderState.baseModelVariant % 2 == 0) {
                this.leftArm.xRot = -2.67F + wave * 0.25F;
                this.leftArm.zRot = 0.2F - wave * 0.4F;
            } else {
                this.rightArm.xRot = -2.67F + wave * 0.25F;
                this.rightArm.zRot = -0.2F + wave * 0.4F;
            }
        }
    }

    @Override
    public void translateToArms(TouristRenderState entityRenderState, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.arms.translateAndRotate(poseStack);
    }
}
