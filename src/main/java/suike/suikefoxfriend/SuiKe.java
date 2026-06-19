package suike.suikefoxfriend;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import suike.suikefoxfriend.api.IOwnable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import suike.suikefoxfriend.payloads.AskClientForModPayload;

public class SuiKe implements ModInitializer {
	public static final String MOD_ID = "suikefoxfriend";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static List<Item> foxCanPickupItemList = Lists.newArrayList(
        Items.TOTEM_OF_UNDYING, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.COPPER_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.NETHERITE_SWORD, Items.WOODEN_SWORD
    );
	public static List<Item> foxNotPickupItemList = Lists.newArrayList(
        Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.SUSPICIOUS_STEW
    );

    @Override
    public void onInitialize() {

        //This whole commented out thing is meant to be a system to ensure that the mod is needed both server-side and client-side. This is way beyond my capabilities for the moment though
//        PayloadTypeRegistry.clientboundPlay().register(AskClientForModPayload.TYPE, AskClientForModPayload.CODEC);
//
//        ServerLoginNetworking.registerGlobalReceiver(Identifier.fromNamespaceAndPath(MOD_ID, "clientside_mod_check_send"), (server, handler, understood, buf, sync, response) -> {
//            Packet packet = response.createPacket(new AskClientForModPayload(true));
//            response.sendPacket(packet);
//        });
//
//        ClientLoginNetworking.registerGlobalReceiver(Identifier.fromNamespaceAndPath(MOD_ID, "clientside_mod_check_send"), (client, handler, buf, listener) -> {
//            if (buf.readBoolean()) {
//                CompletableFuture<Boolean> future = new CompletableFuture<>();
//                future.complete(Boolean.TRUE);
//                return future;
//            }
//            return null;
//        });

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