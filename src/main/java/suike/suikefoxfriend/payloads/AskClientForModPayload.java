package suike.suikefoxfriend.payloads;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import suike.suikefoxfriend.SuiKe;

//Currently unused.
public record AskClientForModPayload(boolean isSentByServer) implements CustomPacketPayload {

    public static final Identifier ASK_CLIENT_FOR_MOD_PAYLOAD_ID = Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "ask_client_for_mod");

    public static final CustomPacketPayload.Type<AskClientForModPayload> TYPE = new CustomPacketPayload.Type<>(ASK_CLIENT_FOR_MOD_PAYLOAD_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, Boolean> BOOLEAN_CODEC = new StreamCodec<RegistryFriendlyByteBuf, Boolean>() {

        public Boolean decode(final RegistryFriendlyByteBuf input) {return input.readBoolean();}

        public void encode(final RegistryFriendlyByteBuf output, final Boolean value) {output.writeBoolean(value);}
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, AskClientForModPayload> CODEC = StreamCodec.composite(BOOLEAN_CODEC, AskClientForModPayload::isSentByServer, AskClientForModPayload::new);


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
