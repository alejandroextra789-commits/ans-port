package com.ans.client.bedrock;

import software.bernie.geckolib.core.object.DataTicket;
import java.util.List;

public record FrameResult(MobPose pose, List<RenderLayer> layers, float scaleX, float scaleY, float scaleZ) {
   public static final DataTicket<FrameResult> TICKET = new DataTicket<>("ans_frame", FrameResult.class);
}
