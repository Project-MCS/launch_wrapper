/*
 * Copyright 2017 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcbounds.launch.mixin;

import net.minecraft.server.v1_16_R3.DedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DedicatedServer.class, remap = false)
public class MixinDedicatedServer {
    
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            LogManager.getLogger().info(Bukkit.getServer().getName() + " successfully bootstrapped.");
        }
    }
}