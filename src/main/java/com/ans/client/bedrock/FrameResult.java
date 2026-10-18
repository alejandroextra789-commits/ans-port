package com.ans.client.bedrock;

import com.geckolib.constant.dataticket.DataTicket;
import java.util.List;

public record FrameResult(MobPose pose, List<RenderLayer> layers, float scaleX, float scaleY, float scaleZ) {
   public static final DataTicket<FrameResult> TICKET = DataTicket.create("ans_frame", FrameResult.class);
}
