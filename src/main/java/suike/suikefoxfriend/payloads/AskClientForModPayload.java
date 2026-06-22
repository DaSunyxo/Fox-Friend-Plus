package suike.suikefoxfriend.payloads;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import suike.suikefoxfriend.SuiKe;

import java.nio.charset.Charset;

import static net.minecraft.core.UUIDUtil.STRING_CODEC;

//Currently unused.
public record AskClientForModPayload(short message) implements CustomPacketPayload {

    public static final Identifier ASK_CLIENT_FOR_MOD_PAYLOAD_ID = Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "ask_client_for_mod");

    public static final CustomPacketPayload.Type<AskClientForModPayload> TYPE = new CustomPacketPayload.Type<>(ASK_CLIENT_FOR_MOD_PAYLOAD_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, Short> SHORT_CODEC = new StreamCodec<RegistryFriendlyByteBuf, Short>() {

        public Short decode(final RegistryFriendlyByteBuf input) {return input.readShort();}

        public void encode(final RegistryFriendlyByteBuf output, final Short value) {output.writeShort(value);}
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, AskClientForModPayload> CODEC = StreamCodec.composite(SHORT_CODEC, AskClientForModPayload::message, AskClientForModPayload::new);


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
