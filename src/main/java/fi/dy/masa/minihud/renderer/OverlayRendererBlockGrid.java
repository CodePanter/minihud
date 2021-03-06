package fi.dy.masa.minihud.renderer;

import org.lwjgl.opengl.GL11;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.malilib.render.RenderObjectBase;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.BlockGridMode;

public class OverlayRendererBlockGrid extends OverlayRendererBase
{
    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return RendererToggle.OVERLAY_BLOCK_GRID.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        if (this.lastUpdatePos == null)
        {
            return true;
        }

        return Math.abs(entity.posX - this.lastUpdatePos.getX()) > 8 ||
               Math.abs(entity.posY - this.lastUpdatePos.getY()) > 8 ||
               Math.abs(entity.posZ - this.lastUpdatePos.getZ()) > 8;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, Minecraft mc)
    {
        Color4f color = Configs.Colors.BLOCK_GRID_OVERLAY_COLOR.getColor();
        int radius = Configs.Generic.BLOCK_GRID_OVERLAY_RADIUS.getIntegerValue();

        RenderObjectBase renderLines = this.renderObjects.get(0);
        BUFFER_1.begin(renderLines.getGlMode(), DefaultVertexFormats.POSITION_COLOR);
        BlockGridMode mode = Configs.Generic.BLOCK_GRID_OVERLAY_MODE.getOptionListValue();

        switch (mode)
        {
            case ALL:
                this.renderLinesAll(cameraPos, this.lastUpdatePos, radius, color, BUFFER_1);
                break;
            case NON_AIR:
                this.renderLinesNonAir(cameraPos, entity.getEntityWorld(), this.lastUpdatePos, radius, color, BUFFER_1);
                break;
            case ADJACENT:
                this.renderLinesAdjacentToNonAir(cameraPos, entity.getEntityWorld(), this.lastUpdatePos, radius, color, BUFFER_1);
                break;
        }

        BUFFER_1.finishDrawing();
        renderLines.uploadData(BUFFER_1);
    }

    @Override
    public void allocateGlResources()
    {
        this.allocateBuffer(GL11.GL_LINES);
    }

    protected void renderLinesAll(Vec3d cameraPos, BlockPos center, int radius, Color4f color, BufferBuilder buffer)
    {
        final double startX = center.getX() - radius - cameraPos.x;
        final double startY = center.getY() - radius - cameraPos.y;
        final double startZ = center.getZ() - radius - cameraPos.z;
        final double endX = center.getX() + radius - cameraPos.x;
        final double endY = center.getY() + radius - cameraPos.y;
        final double endZ = center.getZ() + radius - cameraPos.z;

        for (double x = startX; x <= endX; x += 1.0D)
        {
            for (double y = startY; y <= endY; y += 1.0D)
            {
                buffer.pos(x, y, startZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(x, y, endZ  ).color(color.r, color.g, color.b, color.a).endVertex();
            }
        }

        for (double x = startX; x <= endX; x += 1.0D)
        {
            for (double z = startZ; z <= endZ; z += 1.0D)
            {
                buffer.pos(x, startY, z).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(x, endY  , z).color(color.r, color.g, color.b, color.a).endVertex();
            }
        }

        for (double z = startZ; z <= endZ; z += 1.0D)
        {
            for (double y = startY; y <= endY; y += 1.0D)
            {
                buffer.pos(startX, y, z).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(endX  , y, z).color(color.r, color.g, color.b, color.a).endVertex();
            }
        }
    }

    protected void renderLinesNonAir(Vec3d cameraPos, World world, BlockPos center, int radius, Color4f color, BufferBuilder buffer)
    {
        final int startX = center.getX() - radius;
        final int startY = center.getY() - radius;
        final int startZ = center.getZ() - radius;
        final int endX = center.getX() + radius;
        final int endY = center.getY() + radius;
        final int endZ = center.getZ() + radius;
        int lastCX = startX >> 4;
        int lastCZ = startZ >> 4;
        Chunk chunk = world.getChunk(lastCX, lastCZ);
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (int x = startX; x <= endX; ++x)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                int cx = x >> 4;
                int cz = z >> 4;

                if (cx != lastCX || cz != lastCZ)
                {
                    chunk = world.getChunk(cx, cz);
                    lastCX = cx;
                    lastCZ = cz;
                }

                int height = chunk.getHeightValue(x & 0xF, z & 0xF);

                for (int y = startY; y <= endY; ++y)
                {
                    if (y > height)
                    {
                        break;
                    }

                    posMutable.setPos(x, y, z);

                    if (chunk.getBlockState(x, y, z).getMaterial() != Material.AIR)
                    {
                        fi.dy.masa.malilib.render.RenderUtils.drawBlockSpaceAllOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, buffer);
                    }
                }
            }
        }
    }

    protected void renderLinesAdjacentToNonAir(Vec3d cameraPos, World world, BlockPos center, int radius, Color4f color, BufferBuilder buffer)
    {
        final int startX = center.getX() - radius;
        final int startY = center.getY() - radius;
        final int startZ = center.getZ() - radius;
        final int endX = center.getX() + radius;
        final int endY = center.getY() + radius;
        final int endZ = center.getZ() + radius;
        int lastCX = startX >> 4;
        int lastCZ = startZ >> 4;
        Chunk chunk = world.getChunk(lastCX, lastCZ);
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posMutable2 = new BlockPos.MutableBlockPos();

        for (int x = startX; x <= endX; ++x)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                int cx = x >> 4;
                int cz = z >> 4;

                if (cx != lastCX || cz != lastCZ)
                {
                    chunk = world.getChunk(cx, cz);
                    lastCX = cx;
                    lastCZ = cz;
                }

                for (int y = startY; y <= endY; ++y)
                {
                    posMutable.setPos(x, y, z);

                    if (chunk.getBlockState(posMutable).getMaterial() == Material.AIR)
                    {
                        for (EnumFacing side : PositionUtils.VERTICAL_DIRECTIONS)
                        {
                            posMutable2.setPos(
                                    posMutable.getX() + side.getXOffset(),
                                    posMutable.getY() + side.getYOffset(),
                                    posMutable.getZ() + side.getZOffset());

                            if (chunk.getBlockState(posMutable2).getMaterial() != Material.AIR)
                            {
                                fi.dy.masa.malilib.render.RenderUtils.drawBlockSpaceAllOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, buffer);
                                break;
                            }
                        }

                        for (EnumFacing side : PositionUtils.HORIZONTAL_DIRECTIONS)
                        {
                            posMutable2.setPos(
                                    posMutable.getX() + side.getXOffset(),
                                    posMutable.getY() + side.getYOffset(),
                                    posMutable.getZ() + side.getZOffset());

                            if (world.getBlockState(posMutable2).getMaterial() != Material.AIR)
                            {
                                fi.dy.masa.malilib.render.RenderUtils.drawBlockSpaceAllOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, buffer);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
