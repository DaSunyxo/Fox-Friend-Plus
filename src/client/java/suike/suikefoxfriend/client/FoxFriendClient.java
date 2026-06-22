package suike.suikefoxfriend.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import suike.suikefoxfriend.SuiKe;

import java.util.concurrent.CompletableFuture;

import static suike.suikefoxfriend.SuiKe.MOD_ID;

public class FoxFriendClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientLoginNetworking.registerGlobalReceiver(Identifier.fromNamespaceAndPath(MOD_ID, "clientside_mod_check"), (client, handler, buf, listener) -> {
			short message = buf.readShort();

			if (message == 96) {
				FriendlyByteBuf response = FriendlyByteBufs.create();
				response.writeShort((short) 96);
				SuiKe.LOGGER.warn("Received packet from server!");
				return CompletableFuture.completedFuture(response);
			}

			return null;
		});
	}
}