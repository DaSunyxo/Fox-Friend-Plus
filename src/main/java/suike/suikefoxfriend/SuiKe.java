package suike.suikefoxfriend;

import java.util.List;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import suike.suikefoxfriend.api.IOwnable;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
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

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand == Hand.MAIN_HAND && !world.isClient()) {
                // 检查是否是右键点击
				if (entity instanceof FoxEntity foxEntity) {
                    IOwnable foxIOwnable = ((IOwnable) entity);
                    Item handItem = player.getMainHandStack().getItem();
					if (foxIOwnable.isTamed() && handItem != Items.SWEET_BERRIES && handItem != Items.NAME_TAG && handItem != Items.LEAD) {
                        //If player is sneak right-clicking, drop item in mouth
                        if (player.isSneaking() && player.equals(foxIOwnable.getOwner())) {
                            ItemStack stack = foxEntity.getEquippedStack(EquipmentSlot.MAINHAND);
                            if (!stack.equals(ItemStack.EMPTY)) {
                                ItemStack droppedStack = stack;
                                foxEntity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                                ItemEntity itemEntity = new ItemEntity(
                                        foxEntity.getEntityWorld(), foxEntity.getX() + foxEntity.getRotationVector().x, foxEntity.getY() + 1.0, foxEntity.getZ() + foxEntity.getRotationVector().z, droppedStack
                                );
                                itemEntity.setPickupDelay(20);
                                itemEntity.setThrower(foxEntity);
                                foxEntity.playSound(SoundEvents.ENTITY_FOX_SPIT, 1.0F, 1.0F);
                                foxEntity.getEntityWorld().spawnEntity(itemEntity);
                                player.swingHand(Hand.MAIN_HAND, true);
                            }
                        }
                        // 设置等待状态
                        else {
                            foxIOwnable.playerSetWaiting(player);
                            return ActionResult.SUCCESS;
                        }

					} else if (!foxIOwnable.isTamed() && handItem == Items.SWEET_BERRIES) {
                        if (!player.getAbilities().creativeMode) {
                            player.getMainHandStack().decrement(1);
                        }

                        // 设为驯服
                        foxIOwnable.playerTamedFox(player);
						return ActionResult.SUCCESS;
                    }
				}
            }
            return ActionResult.PASS;
        });
    }
}