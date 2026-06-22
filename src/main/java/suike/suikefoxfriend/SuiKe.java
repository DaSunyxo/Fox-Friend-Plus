package suike.suikefoxfriend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Runnables;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import suike.suikefoxfriend.api.IOwnable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class SuiKe implements ModInitializer {
	public static final String MOD_ID = "suikefoxfriend";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static List<Item> foxCanPickupItemList = Lists.newArrayList(
        Items.TOTEM_OF_UNDYING, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.COPPER_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.NETHERITE_SWORD, Items.WOODEN_SWORD
    );
	public static List<Item> foxNotPickupItemList = Lists.newArrayList(
        Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.SUSPICIOUS_STEW
    );

    private static ArrayList<String> pendingLogins;

    @Override
    public void onInitialize() {

        pendingLogins = new ArrayList<>();

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) -> {

            LOGGER.warn(handler.getUserName() + " is trying to connect. If they fail with the error message \"java.lang.IndexOutOfBoundsException\", please ensure that they are using the latest version of Fox Friend Remastered.");

            FriendlyByteBuf buf = FriendlyByteBufs.create();
            buf.writeShort((short) 96);
            sender.sendPacket(Identifier.fromNamespaceAndPath(MOD_ID, "clientside_mod_check"), buf);

        });

        ServerLoginNetworking.registerGlobalReceiver(Identifier.fromNamespaceAndPath(MOD_ID, "clientside_mod_check"), (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (buf.readShort() == 96) return;
        });


        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand == InteractionHand.MAIN_HAND && !world.isClientSide()) {
                // 检查是否是右键点击
				if (entity instanceof Fox foxEntity) {
                    IOwnable foxIOwnable = ((IOwnable) entity);
                    Item handItem = player.getMainHandItem().getItem();
					if (foxIOwnable.isTamed() && handItem != Items.SWEET_BERRIES && handItem != Items.NAME_TAG && handItem != Items.LEAD) {
                        //If player is sneak right-clicking, drop item in mouth
                        if (player.isCrouching() && player.equals(foxIOwnable.getOwner())) {
                            ItemStack stack = foxEntity.getMainHandItem();
                            if (!stack.equals(ItemStack.EMPTY)) {
                                ItemStack droppedStack = stack;
                                foxEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                                ItemEntity itemEntity = new ItemEntity(
                                        foxEntity.level(), foxEntity.getX() + foxEntity.getRotationVector().x, foxEntity.getY() + 1.0, foxEntity.getZ(), droppedStack
                                );
                                itemEntity.setPickUpDelay(20);
                                itemEntity.setThrower(foxEntity);
                                foxEntity.playSound(SoundEvents.FOX_EAT, 1.0F, 1.0F);
                                foxEntity.level().addFreshEntity(itemEntity);
                                player.swing(InteractionHand.MAIN_HAND, true);
                            }
                        }
                        // 设置等待状态
                        else {
                            foxIOwnable.playerSetWaiting(player);
                            return InteractionResult.SUCCESS;
                        }

					} else if (!foxIOwnable.isTamed() && handItem == Items.SWEET_BERRIES) {
                        if (!player.gameMode().isCreative()) {
                            player.getMainHandItem().setCount(player.getMainHandItem().count() - 1);
                        }

                        // 设为驯服
                        foxIOwnable.playerTamedFox(player);
                        player.swing(InteractionHand.MAIN_HAND, true);
						return InteractionResult.SUCCESS;
                    }
				}
            }
            return InteractionResult.PASS;
        });
    }
}