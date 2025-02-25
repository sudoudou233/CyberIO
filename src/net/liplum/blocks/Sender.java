package net.liplum.blocks;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockGroup;
import net.liplum.animations.AniConfig;
import net.liplum.animations.AniState;
import net.liplum.animations.AutoAnimation;
import net.liplum.animations.IAnimated;
import net.liplum.api.IDataReceiver;
import net.liplum.api.IDataSender;
import net.liplum.utils.AtlasUtil;
import net.liplum.utils.GraphicUtl;

public class Sender extends AniBlock<Sender, Sender.SenderBuild> {
    private final int UploadAnimFrameNumber = 7;
    public TextureRegion coverTR;
    public TextureRegion upArrowTR;
    public TextureRegion crossTR;
    private AniState<Sender, SenderBuild> IdleAni;
    private AniState<Sender, SenderBuild> UploadAni;
    private AniState<Sender, SenderBuild> blockedAni;
    private IAnimated UploadAnim;

    public Sender(String name) {
        super(name);
        solid = true;
        update = true;
        acceptsItems = true;
        configurable = true;
        group = BlockGroup.transportation;

        config(Integer.class, SenderBuild::setReceiverPackedPos);
    }

    public void genAnimState() {
        IdleAni = addAniState(new AniState<>("Idle", ((sender, build) -> {

        })));
        UploadAni = addAniState(new AniState<>("Upload", (sender, build) -> {
            UploadAnim.draw(Color.green, build.x, build.y);
        }));
        blockedAni = addAniState(new AniState<>("Blocked", ((sender, build) -> {
            Draw.color(Color.red);
            Draw.rect(sender.crossTR,
                    build.x, build.y
            );
            Draw.color();
        })));
    }

    public void genAniConfig() {
        aniConfig = new AniConfig<>();
        aniConfig.defaultState(IdleAni);
        aniConfig.enter(IdleAni, UploadAni, (block, build) -> {
            IDataReceiver reb = build.getReceiverBuilding();
            if (reb != null) {
                return reb.canAcceptAnyData(build);
            }
            return false;
        }).enter(IdleAni, blockedAni, (block, build) -> {
            IDataReceiver reb = build.getReceiverBuilding();
            if (reb != null) {
                return !reb.isOutputting() && !reb.canAcceptAnyData(build);
            }
            return false;
        });

        aniConfig.enter(UploadAni, IdleAni, (block, build) ->
                build.getReceiverPackedPos() == -1
        ).enter(UploadAni, blockedAni, (block, build) -> {
            IDataReceiver reb = build.getReceiverBuilding();
            if (reb != null) {
                return !reb.isOutputting() && !reb.canAcceptAnyData(build);
            }
            return false;
        });

        aniConfig.enter(blockedAni, IdleAni, (block, build) ->
                build.getReceiverPackedPos() == -1
        ).enter(blockedAni, UploadAni, ((block, build) -> {
            IDataReceiver reb = build.getReceiverBuilding();
            if (reb != null) {
                return reb.isOutputting() || reb.canAcceptAnyData(build);
            }
            return false;
        }));
        aniConfig.build();
    }

    @Override
    public void load() {
        super.load();
        coverTR = AtlasUtil.sub(this, "cover");
        upArrowTR = AtlasUtil.sub(this, "up-arrow");
        crossTR = AtlasUtil.sub(this, "cross");
        loadAnimation();
    }

    public void loadAnimation() {
        UploadAnim = new AutoAnimation(30f, AtlasUtil.animation(this, "up-arrow", true, UploadAnimFrameNumber));
    }

    public class SenderBuild extends AniBuild implements IDataSender {
        private int receiverPackedPos = -1;

        public int getReceiverPackedPos() {
            return receiverPackedPos;
        }

        public void setReceiverPackedPos(int receiverPackedPos) {
            this.receiverPackedPos = receiverPackedPos;
        }

        private void checkReceiverPos() {
            if (getReceiverPackedPos() != -1 && getReceiverBuilding() == null) {
                setReceiverPackedPos(-1);
            }
        }

        @Override
        public void fixedUpdateTile() {
            checkReceiverPos();
        }

        @Override
        public void sendData(IDataReceiver receiver, Item item) {
            receiver.receiveData(this, item);
        }

        @Override
        public void drawSelect() {
            float sin = Mathf.absin(Time.time, 6f, 1f);
            IDataReceiver dr = this.getReceiverBuilding();
            if (dr != null) {
                Tile ret = dr.getTile();
                Building reb = dr.getBuilding();
                Block reblock = dr.getBlock();
                float retdrawx = ret.drawx();
                float retdrawy = ret.drawy();
                Drawf.dashCircle(retdrawx, retdrawy,
                        (reblock.size / 2f + 1) * Vars.tilesize + sin - 2f,
                        Pal.place);
                GraphicUtl.drawDashLineBetweenTwoBlocks(this.block, this.tile.x, this.tile.y,
                        reblock, ret.worldx(), ret.worldy());
            }
        }

        @Override
        public void handleItem(Building source, Item item) {
            IDataReceiver reb = this.getReceiverBuilding();
            if (reb != null) {
                this.sendData(reb, item);
            }
        }

        @Nullable
        public IDataReceiver getReceiverBuilding() {
            if (getReceiverPackedPos() != -1) {
                Building rBuild = Vars.world.build(getReceiverPackedPos());
                if (rBuild instanceof IDataReceiver) {
                    return (IDataReceiver) rBuild;
                }
            }
            return null;
        }

        @Override
        public void drawConfigure() {
            float sin = Mathf.absin(Time.time, 6f, 1f);
            Draw.color(Pal.accent);
            Lines.stroke(1f);
            Drawf.circles(tile.drawx(), tile.drawy(), (tile.block().size / 2f + 1) * Vars.tilesize + sin - 2f, Pal.accent);
            IDataReceiver reb = this.getReceiverBuilding();
            if (reb != null) {
                Tile ret = reb.getTile();
                Drawf.dashCircle(ret.drawx(), ret.drawy(),
                        (ret.block().size / 2f + 1) * Vars.tilesize + sin - 2f, Pal.accent);
            }
        }

        public void clearReceiver() {
            configure(-1);
        }

        @Override
        public boolean onConfigureTileTapped(Building other) {
            if (this == other) {
                this.clearReceiver();
                return false;
            }

            if (this.getReceiverPackedPos() == other.pos()) {
                this.clearReceiver();
                return false;
            }
            if (other instanceof IDataReceiver) {
                this.setReceiver((IDataReceiver) other);
                return false;
            }
            return true;
        }

        public void setReceiver(IDataReceiver receiver) {
            setReceiverPackedPos(receiver.getBuilding().pos());
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            if (Mathf.zero(power.status)) {
                return false;
            }
            IDataReceiver reb = this.getReceiverBuilding();
            if (reb != null) {
                return reb.acceptData(this, item);
            }
            return false;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(this.receiverPackedPos);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            this.receiverPackedPos = read.i();
        }

        @Override
        public Building getBuilding() {
            return this;
        }

        @Override
        public Tile getTile() {
            return tile();
        }

        @Override
        public Block getBlock() {
            return block();
        }
    }
}