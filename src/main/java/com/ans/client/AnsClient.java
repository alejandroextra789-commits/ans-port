package com.ans.client;

import com.ans.Ans;
import com.ans.client.attachable.AttachableQueries;
import com.ans.client.attachable.Attachables;
import com.ans.client.dev.DevSelfTest;
import com.ans.client.dev.ScenarioTest;
import com.ans.client.gecko.AnsGeckoLoader;
import com.ans.client.mob.AnimatedMobs;
import com.ans.client.mob.AnsMobs;
import com.ans.client.molang.BedrockQueries;
import com.ans.client.particle.BedrockParticles;
import com.ans.client.settings.AnsSettings;
import com.ans.client.sound.PackSounds;
import com.ans.client.sound.SoundMuting;
import net.minecraftforge.common.MinecraftForge;

public final class AnsClient {
   private AnsClient() {}

   public static void initialize() {
      BedrockQueries.install();
      AttachableQueries.install();
      Attachables.installHook();
      AnsGeckoLoader.install();
      AnsMobs.registerAll();
      AnimatedMobs.registerRenderers();
      PackSounds.init();
      MinecraftForge.EVENT_BUS.addListener(SoundMuting::onPlaySound);
      BedrockParticles.init();
      AnsSettings.init();
      DevSelfTest.init();
      ScenarioTest.init();
      Ans.LOGGER.info("[ans] Actions & Stuff (Java port) client initialised");
   }

}
