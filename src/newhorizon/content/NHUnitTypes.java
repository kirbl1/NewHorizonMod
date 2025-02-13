package newhorizon.content;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.BuilderAI;
import mindustry.ai.types.MinerAI;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Lightning;
import mindustry.entities.Units;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.part.HaloPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.part.ShapePart;
import mindustry.entities.pattern.*;
import mindustry.entities.units.WeaponMount;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.ammo.ItemAmmoType;
import mindustry.type.ammo.PowerAmmoType;
import mindustry.type.weapons.PointDefenseWeapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.Env;
import newhorizon.NHSetting;
import newhorizon.NewHorizon;
import newhorizon.expand.bullets.*;
import newhorizon.expand.entities.UltFire;
import newhorizon.expand.units.*;
import newhorizon.util.feature.PosLightning;
import newhorizon.util.func.NHFunc;
import newhorizon.util.func.NHInterp;
import newhorizon.util.func.NHPixmap;
import newhorizon.util.graphic.DrawFunc;
import newhorizon.util.graphic.OptionalMultiEffect;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.randLenVectors;
import static mindustry.Vars.tilePayload;
import static mindustry.Vars.tilesize;

public class NHUnitTypes{
	public static final Color OColor = Color.valueOf("565666");
	
	public static final byte
			ANCIENT_GROUND = 10, ANCIENT_AIR = 11,
			OTHERS = Byte.MIN_VALUE,
			GROUND_LINE_1 = 0, AIR_LINE_1 = 1, AIR_LINE_2 = 2, ENERGY_LINE_1 = 3, NAVY_LINE_1 = 6;
	
	public static Weapon
		basicCannon, laserCannon;
	
	public static Weapon
			ancientSecTurret,
			posLiTurret, closeAATurret, collapserCannon, collapserLaser, multipleLauncher, smallCannon,
			mainCannon, pointDefenceWeaponC;
	
	public static NHUnitType
			guardian, //Energy
			gather, saviour, rhino, //Air-Assist
			pester, laugra, //ancient
			assaulter, anvil, collapser, //Air-2
			origin, thynomo, aliotiat, tarlidor, annihilation, sin, //Ground-1
			sharp, branch, warper/*, striker*/, naxos, destruction, longinus, hurricane, //Air-1
			/*relay,*/ ghost, zarkov, declining; //Navy
	
	static{
			EntityMapping.nameMap.put(NewHorizon.name("declining"), EntityMapping.idMap[20]);
			EntityMapping.nameMap.put(NewHorizon.name("zarkov"), EntityMapping.idMap[20]);
			EntityMapping.nameMap.put(NewHorizon.name("ghost"), EntityMapping.idMap[20]);
			EntityMapping.nameMap.put(NewHorizon.name("relay"), EntityMapping.idMap[20]);

			EntityMapping.nameMap.put(NewHorizon.name("saviour"), EntityMapping.idMap[5]);
			EntityMapping.nameMap.put(NewHorizon.name("air-raid-missile"), AdaptedTimedKillUnit::new);
		
			EntityMapping.nameMap.put(NewHorizon.name("origin"), EntityMapping.idMap[4]);
			EntityMapping.nameMap.put(NewHorizon.name("thynomo"), EntityMapping.idMap[4]);
			EntityMapping.nameMap.put(NewHorizon.name("aliotiat"), EntityMapping.idMap[4]);
			EntityMapping.nameMap.put(NewHorizon.name("tarlidor"), EntityMapping.idMap[4]);
			EntityMapping.nameMap.put(NewHorizon.name("annihilation"), EntityMapping.idMap[4]);
			EntityMapping.nameMap.put(NewHorizon.name("sin"), EntityMapping.idMap[4]);
			EntityMapping.nameMap.put(NewHorizon.name("pester"), PesterEntity::new);
			EntityMapping.nameMap.put(NewHorizon.name("laugra"), EntityMapping.idMap[43]);

			EntityMapping.nameMap.put(NewHorizon.name("guardian"), EnergyUnit::new);
		}
	
	public static class NHUnitType extends UnitType{
		public NHUnitType(String name){
			super(name);
			
			ammoType = new ItemAmmoType(NHItems.presstanium);
		}
		
		public void setRequirements(ItemStack[] stacks){
			cachedRequirements = stacks;
			totalRequirements = firstRequirements = ItemStack.mult(stacks, 1 / 15f);
		}
		
		public void setBuildCost(ItemStack[] stacks){
		}
	}
		
	public static Weapon copyAnd(Weapon weapon, Cons<Weapon> modifier){
		Weapon n = weapon.copy();
		modifier.get(n);
		return n;
	}
	
	public static Weapon copyAndMove(Weapon weapon, float x, float y){
		Weapon n = weapon.copy();
		n.x = x;
		n.y = y;
		return n;
	}
	
	public static Weapon copyAndMoveAnd(Weapon weapon, float x, float y, Cons<Weapon> modifier){
		Weapon n = weapon.copy();
		n.x = x;
		n.y = y;
		modifier.get(n);
		return n;
	}
	
	public static Seq<StatusEffect> statuses;
	
	public static void immunise(UnitType type){
		if(statuses == null){
			statuses = Vars.content.statusEffects().copy();
			statuses.filter(s -> {
				return s.disarm || s.damage > 0 || s.healthMultiplier * s.reloadMultiplier * s.buildSpeedMultiplier < 1;
			});
			statuses.add(NHStatusEffects.scannerDown);
			statuses.add(StatusEffects.wet);
		}
		
		type.immunities.addAll(statuses);
	}
	
	private static void loadPreviousWeapon(){
		ancientSecTurret = new Weapon(NewHorizon.name("pester-secondary-laser")){{
			shootY = 3f;
			shootX = -1f;
			
			recoil = 1f;
			
			reload = 16f;
			mirror = rotate = true;
			rotateSpeed = 4f;
			autoTarget = true;
			controllable = false;
			
			shootSound = NHSounds.gauss;
			
			bullet = new PosLightningType(120f){{
				maxRange = 160f;
				lifetime = 48f;
				boltNum = 1;
				hitEffect = NHFx.hitSpark;
				hitColor = lightColor = lightningColor = NHColor.ancient;
			}};
		}};
		
		laserCannon = new Weapon(NewHorizon.name("laser-cannon")){{
			mirror = top = alternate = autoTarget = rotate = true;
			predictTarget = controllable = false;
			x = 22;
			y = -50;
			reload = 12f;
			recoil = 3f;
			inaccuracy = 0;
			shoot = new ShootPattern();
			rotateSpeed = 25f;
			shootSound = NHSounds.gauss;
			bullet = new ShrapnelBulletType(){{
				lifetime = 45f;
				length = 200f;
				damage = 180.0F;
				status = StatusEffects.shocked;
				statusDuration = 60f;
				fromColor = NHColor.lightSkyFront;
				toColor = NHColor.lightSkyBack;
				serrationSpaceOffset = 40f;
				width = 6f;
				shootEffect = NHFx.lightningHitSmall(NHColor.lightSkyBack);
				smokeEffect = new MultiEffect(NHFx.lightSkyCircleSplash, new Effect(lifetime + 10f, b -> {
					Draw.color(fromColor, toColor, b.fin());
					Fill.circle(b.x, b.y, (width / 1.75f) * b.fout());
				}));
			}};
		}};
		
		pointDefenceWeaponC = new PointDefenseWeapon(NewHorizon.name("cannon")){{
			color = NHColor.lightSkyFront;
			mirror = top = alternate = true;
			reload = 6.0F;
			targetInterval = 6.0F;
			targetSwitchInterval = 6.0F;
			bullet = new BulletType() {
				{
					shootEffect = NHFx.shootLineSmall(color);
					hitEffect = NHFx.lightningHitSmall;
					hitColor = color;
					maxRange = 240.0F;
					damage = 150f;
				}
			};
		}};
		
		mainCannon = new Weapon(NewHorizon.name("main-cannon")){{
			top = rotate = true;
			mirror = false;
			alternate = false;
			cooldownTime = 240f;
			recoil = 7f;
			
			inaccuracy = 4f;
			velocityRnd = 0.075f;
			
			shoot = new ShootPattern(){{
				shots = 3;
				firstShotDelay = 280f;
				shotDelay = 20f;
			}};
			
			rotateSpeed = 1f;
			shootSound = NHSounds.flak2;
			shootCone = 5f;
			shootY = 15f;
			reload = 300f;
			shake = 7f;
			ejectEffect = Fx.blastsmoke;
			bullet = NHBullets.declineProjectile;
		}
			@Override
			public void draw(Unit unit, WeaponMount mount){
				super.draw(unit, mount);
				
				if(!unit.isLocal())return;
				
				float
						z = Draw.z(),
						rotation = unit.rotation - 90f,
						weaponRotation  = rotation + (rotate ? mount.rotation : 0),
						fin = Mathf.clamp(1 - (mount.reload - 10f) / reload),
						wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, recoil),
						wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, recoil);
				
				if(fin == 1f)return;
				
				TextureRegion arrowRegion = NHContent.arrowRegion;
				
				Draw.z(Layer.bullet);
				Draw.color(bullet.hitColor);
				
				float railF = Mathf.curve(Interp.pow2Out.apply(fin), 0f, 0.25f) * Mathf.curve(Interp.pow4Out.apply(1 - fin), 0f, 0.1f) * fin;
				
				float length = Math.min(bullet.range, Mathf.dst(wx, wy, unit.aimX, unit.aimY));
				float spacing = 25f;
				for(int i = 0; i <= length / spacing; i++){
					Tmp.v1.trns(weaponRotation + 90f, i * spacing * Mathf.curve(Interp.pow4Out.apply(1 - fin), 0f, 0.1f) + shootY);
					float f = Interp.pow3Out.apply(Mathf.clamp((fin * length - i * spacing) / spacing)) * (0.6f + railF * 0.4f) * 0.8f;
					Draw.rect(arrowRegion, wx + Tmp.v1.x, wy + Tmp.v1.y, arrowRegion.width * Draw.scl * f, arrowRegion.height * Draw.scl * f, weaponRotation);
				}
				
				Tmp.v1.trns(weaponRotation + 90f, 0f, (2 - railF) * 5f);
				Tmp.v2.trns(weaponRotation + 90f, shootY);
				Lines.stroke(railF * 2f);
				for(int i : Mathf.signs){
					Lines.lineAngle(wx + Tmp.v1.x * i + Tmp.v2.x, wy + Tmp.v1.y * i + Tmp.v2.y, weaponRotation + 90f, length * (0.75f + railF / 4f) * Mathf.curve(Interp.pow5Out.apply(1 - fin) * Mathf.curve(Interp.pow4Out.apply(1 - fin), 0f, 0.1f), 0f, 0.1f));
				}
				
				Draw.reset();
				Draw.z(z);
			}
		};
		
		multipleLauncher = new Weapon(NewHorizon.name("mult-launcher")){{
			reload = 60f;
			
			shoot = new ShootPattern(){{
				shots = 3;
				shotDelay = 8f;
			}};
			
			shake = 3f;
			
			shootX = 2;
			xRand = 5;
			
			mirror = true;
			rotateSpeed = 2.5f;
			alternate = true;
			shootSound = NHSounds.launch;
			shootCone = 30f;
			shootY = 5f;
			top = true;
			rotate = true;
			bullet = new BasicBulletType(5.25f, 150f, NHBullets.STRIKE){{
				lifetime = 50;
				
				knockback = 12f;
				width = 11f;
				height = 28f;
				
				trailWidth = 2.2f;
				trailLength = 20;
				drawSize = 300f;
				
				
				homingDelay = 5f;
				homingPower = 0.0075f;
				homingRange = 140f;
				
				splashDamageRadius = 16f;
				splashDamage = damage * 0.75f;
				backColor = lightColor = lightningColor = trailColor = hitColor = NHColor.lightSkyBack;
				frontColor = NHColor.lightSkyFront;
				
				hitEffect = NHFx.circleSplash(backColor, 40f, 4, 40f, 6f);
				despawnEffect = NHFx.hitSparkLarge;
				shootEffect = NHFx.shootCircleSmall(backColor);
				smokeEffect = Fx.shootBigSmoke2;
				
				trailChance = 0.6f;
				trailEffect = NHFx.trailToGray;
				
				hitShake = 3f;
				hitSound = Sounds.plasmaboom;
			}};
		}};
		
		posLiTurret = new Weapon(NewHorizon.name("pos-li-blaster")){{
			shake = 1f;
			shoot = new ShootPattern();
			predictTarget = rotate = false;
			top = alternate = true;
			reload = 45f;
			shootY = 4f;
			shootSound = Sounds.spark;
			heatColor = NHColor.lightSkyBack;
			bullet = new PosLightningType(20f){{
				lightningColor = NHColor.lightSkyBack;
				maxRange = 140f;
				hitEffect = NHFx.lightningHitSmall(lightningColor);
				lightningLength = 1;
				lightningLengthRand = 4;
			}};
		}};
		
		closeAATurret = new Weapon(NewHorizon.name("anti-air-pulse-laser")){{
			shake = 0f;
			shoot = new ShootPattern(){{
				shots = 2;
				shotDelay = 6f;
			}};
			rotate = top = true;
			heatColor = NHColor.lightSkyBack;
			shootSound = NHSounds.laser5;
			shootY = 3f;
			recoil = 2f;
			x = 9.5f;
			y = -7f;
			reload = 25;
			autoTarget = true;
			controllable = predictTarget = false;
			BasicBulletType b = (BasicBulletType)NHBullets.basicSkyFrag;
			b.width *= 0.75f;
			b.height *= 0.75f;
			b.damage *= 0.75f;
			b.trailWidth *= 0.75f;
			bullet = b;
		}};
		
		smallCannon = new Weapon(NewHorizon.name("cannon")){{
			top = mirror = rotate = true;
			reload = 45f;
			
			shoot = new ShootPattern(){{
				shots = 3;
				shotDelay = 8f;
			}};
			controllable = false;
			autoTarget = true;
			shake = 3f;
			inaccuracy = 4f;
			rotateSpeed = 2.5f;
			alternate = true;
			shootSound = NHSounds.scatter;
			shootCone = 15;
			shootY = 5f;
			bullet = new BasicBulletType(5f, 20f){{
				hitColor = trailColor = lightningColor = backColor = lightColor = NHColor.lightSkyBack;
				frontColor = NHColor.lightSkyFront;
				
				width = 8f;
				height = 20f;
				lifetime = 55f;
				
				hitEffect = NHFx.lightningHitSmall(backColor);
				shootEffect = NHFx.shootLineSmall(backColor);
				smokeEffect = Fx.shootSmallSmoke;
				despawnEffect = NHFx.square(backColor, 15f, 2, 14f, 3);
			}
			};
		}};
	}
	
	private static void loadWeapon(){
		basicCannon = new Weapon(NewHorizon.name("basic-weapon")){{
			shoot = new ShootPattern();
			
			shootSound = NHSounds.rapidLaser;
			
			rotateSpeed = 12f;
			reload = 20f;
			shootY = 6f;
			shootX = -1.6f;
			rotate = true;
			mirror = true;
			top = true;
			
			bullet = new BasicBulletType(){{
				width = 8f;
				height = 28f;
				trailWidth = 1f;
				trailLength = 7;
				
				speed = 12f;
				lifetime = 24f;
				drag = 0.015f;
				
				trailColor = hitColor = backColor = lightColor = lightningColor = NHColor.lightSkyBack;
				frontColor = NHColor.lightSkyFront;
				
				damage = 25f;
				
				smokeEffect = Fx.shootSmallSmoke;
				shootEffect = NHFx.shootCircleSmall(backColor);
				despawnEffect = NHFx.circleSplash(backColor, 40f, 3, 18f, 4f);
				hitEffect = NHFx.lightSkyCircleSplash;
			}};
		}};
		
		laserCannon = new Weapon(NewHorizon.name("laser-cannon")){{
			top = autoTarget = rotate = true;
			mirror = alternate = false;
//			predictTarget = controllable = false;
			x = 22;
			y = -50;
			reload = 70f;
			recoil = 1.75f;
			inaccuracy = 2;
			
			shootY = 6;
			
//			layerOffset = 0.0001f;
			
			shoot = new ShootPattern(){{
				shots = 3;
				shotDelay = 12f;
			}};
			
//			parts.add(new RegionPart("-shooter"){{
//				under = true;
//				progress = PartProgress.recoil;
//				x = 0;
//				y = 0;
//				moveY = -3f;
//			}});
			
			shake = 1f;
			rotateSpeed = 18f;
			shootSound = NHSounds.synchro;
			bullet = new BasicBulletType(3.8f, 50){
				{
					speed = 12f;
					trailLength = 12;
					trailWidth = 2f;
					lifetime = 60;
					despawnEffect = NHFx.square45_4_45;
					hitEffect = new Effect(45f, e -> {
						Draw.color(NHColor.lightSkyFront, NHColor.lightSkyBack, e.fin());
						Lines.stroke(1.75f * e.fout());
						if(NHSetting.enableDetails())Lines.spikes(e.x, e.y, 28 * e.finpow(), 5 * e.fout() + 8 * e.fin(NHInterp.parabola4Reversed), 4, 45);
						Lines.square(e.x, e.y, 14 * e.fin(Interp.pow3Out), 45);
					});
					knockback = 4f;
					width = 15f;
					height = 37f;
					lightningDamage = damage * 0.65f;
					backColor = lightColor = lightningColor = trailColor = hitColor = NHColor.lightSkyBack;
					frontColor = NHColor.lightSkyFront;
					lightning = 2;
					lightningLength = lightningLengthRand = 3;
					smokeEffect = Fx.shootBigSmoke2;
					trailChance = 0.2f;
					trailEffect = NHFx.skyTrail;
					drag = 0.015f;
					hitShake = 2f;
					hitSound = Sounds.explosion;
				}
				
				@Override
				public void hit(Bullet b){
					super.hit(b);
					UltFire.createChance(b, 12, 0.05f);
				}
			};
		}};
		
		collapserCannon = new Weapon(NewHorizon.name("collapser-cannon")){{
			top = rotate = alternate = mirror = true;
			reload = 60f;
			shake = 12f;
			heatColor = NHColor.thurmixRed;
			shootSound = NHSounds.laser4;
			shootY = 6f;
			inaccuracy = 0;
			
			shoot = new ShootSpread(){{
				shots = 3;
				shotDelay = 2.25f;
				spread = 1f;
			}};
			
			rotateSpeed = 2f;
			ejectEffect = NHFx.hugeSmoke;
			shootCone = 20f;
			bullet = new AdaptedLaserBulletType(800f){{
				hitColor = NHColor.thurmixRed;
				colors = new Color[]{hitColor.cpy().mul(1f, 1f, 1f, 0.45f), hitColor, NHColor.thurmixRedLight, Color.white};
				length = 600f;
				width = 14f;
				lifetime = PosLightning.lifetime + 5f;
				ammoMultiplier = 4;
				lengthFalloff = 0.8f;
				sideLength = 40f;
				sideWidth = 0.5f;
				sideAngle = 30f;
				largeHit = true;
				hitEffect = NHFx.instHit(hitColor, 2, 36f);
				shootEffect = NHFx.square(hitColor, 15f, 2, 8f, 2f);
			}};
		}};
	}
	
	public static void load(){
		loadWeapon();
		
		loadPreviousWeapon();
		
		laugra = new NHUnitType("laugra"){{
			outlineColor = Pal.darkOutline;
			
			health = 22000;
			armor = 45;
			rotateSpeed = 3f;
			speed = 1.5f;
			
			immunise(this);
			
			squareShape = true;
			omniMovement = false;
			rotateMoveFirst = true;
			envDisabled = Env.none;
			hitSize = 50f;
			
			accel = 0.25f;
			
			treadRects = new Rect[]{new Rect(-85.0F, -104.0F, 28.0F, 208.0F)};
			
			hoverable = hovering = true;
			
			Weapon w = ancientSecTurret.copy();
			w.reload = 12f;
			w.bullet = new BasicBulletType(){{
				width = 9f;
				height = 28f;
				trailWidth = 1.3f;
				trailLength = 7;
				
				speed = 12f;
				lifetime = 24f;
				drag = 0.015f;
				
				trailColor = hitColor = backColor = lightColor = lightningColor = NHColor.ancient;
				frontColor = Color.white;
				
				damage = 40f;
				
				smokeEffect = Fx.shootSmallSmoke;
				shootEffect = NHFx.shootCircleSmall(backColor);
				despawnEffect = NHFx.square45_4_45;
				hitEffect = NHFx.hitSpark;
			}};
			
			weapons.add(copyAndMove(w, 18.5f, 11f), copyAndMove(w, 18.5f, -13f));
			
			weapons.add(new Weapon(name + "-turret"){{
				x = y = 0;
				rotate = true;
				rotateSpeed = 3.5f;
				mirror = alternate = false;
				layerOffset = 0.15f;
				
				shootWarmupSpeed /= 2f;
				minWarmup = 0.9f;
				
				recoil = 2.25f;
				shake = 8f;
				reload = 120f;
				
				shootY = 25;
				
				cooldownTime = 45f;
				heatColor = NHColor.ancientHeat;
				
				shoot = new ShootAlternate(){{
					shots = 2;
					shotDelay = 0f;
					spread = 12.3f;
				}};
				
				inaccuracy = 1.3f;
				shootSound = NHSounds.flak;
				
				bullet = NHBullets.laugraBullet;
				
				abilities.add(new GravityTrapAbility(120f));
				
				parts.add(new RegionPart("-panel"){{
					outline = mirror = true;
					x = y = 0;
					moveX = -2;
					moveRot = -6f;
					moveY = -2f;
					progress = PartProgress.warmup;
				}}, new RegionPart("-barrel"){{
					under = outline = true;
					x = y = 0;
					moveY = -6f;
					progress = PartProgress.recoil;
				}}, new HaloPart(){{
					shapes = 1;
					layer = Layer.bullet + 1f;
					x = 0;
					y = -32f;
					color = NHColor.ancient;
					hollow = true;
					sides = 4;
					stroke = 0f;
					strokeTo = 1.1f;
					radiusTo = 6f;
					rotateSpeed = 1.2f;
					
					progress = PartProgress.warmup.blend(PartProgress.reload, 0.3f);
				}});
				
				parts.add(new HaloPart(){{
					shapes = 1;
					layer = Layer.bullet + 1f;
					x = 14;
					y = -18.5f;
					
					haloRadius = 0;
					
					color = NHColor.ancient;
					tri = mirror = true;
					strokeTo = 2.1f;
					triLength = -0.1f;
					triLengthTo = 16f;
					haloRotation = 315;
					
					progress = PartProgress.warmup;
				}}, new HaloPart(){{
					shapes = 1;
					layer = Layer.bullet + 1f;
					x = 14;
					y = -18.5f;
					haloRadius = 0;
					color = NHColor.ancient;
					tri = mirror = true;
					strokeTo = 2.1f;
					triLength = -0.1f;
					triLengthTo = 3f;
					haloRotation = 135;
					
					progress = PartProgress.warmup;
				}});
			}});
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		pester = new NHUnitType("pester"){{
			
			trailScl = 2f;
			
			outlineRadius += 1;
			outlineColor = Pal.darkOutline;
			
			engineOffset = 45.25f;
			engineSize = -1;
			
			immunise(this);
			
			abilities.add(new MoveEffectAbility(){{
				minVelocity = 0;
				rotateEffect = false;
				effectParam = engineSize;
				parentizeEffects = true;
				teamColor = true;
				display = false;
				
				y = -engineOffset;
				effect = new Effect(33, b -> {
					Draw.color(b.color);
					Angles.randLenVectors(b.id, (int)(b.rotation / 8f), b.rotation / 5f + b.rotation * 2f * b.fin(), (x, y) -> {
						Fill.circle(b.x + x, b.y + y, b.fout() * b.rotation / 2.25f);
					});
				});
			}});
			
			abilities.add(new ShieldArcAbility(){{
				radius = 160f;
				angle = 270;
				regen = 5f;
				cooldown = 60f * 10f;
				max = 50000f;
				width = 15f;
				whenShooting = false;
			}});
			
			class PestEngine extends UnitEngine{
				float triScl = 1;
				
				public PestEngine(float x, float y, float radius, float rotation, float triScl){
					super(x, y, radius, rotation);
					this.triScl = triScl;
				}
				
				public PestEngine(float x, float y, float radius, float rotation){
					super(x, y, radius, rotation);
				}
				
				public void draw(Unit unit){
					UnitType type = unit.type;
					float scale = type.useEngineElevation ? unit.elevation : 1f;
					
					if(scale <= 0.0001f)return;
					
//					float z = Draw.z();
					float rot = unit.rotation - 90;
					
//					Draw.z(Layer.effect + 0.005f);
					Color color = unit.team.color;
					
					Tmp.v1.set(x, y).rotate(rot).add(unit);
					float ex = Tmp.v1.x, ey = Tmp.v1.y;
					
					Draw.color(color);
					Fill.circle(
						ex,
						ey,
						(radius + Mathf.absin(Time.time, 4f, radius / 4f)) * scale
					);
					
					float ang = Time.time * 1.5f;
					for(int i : Mathf.signs){
						DrawFunc.tri(ex, ey, radius / 3f * triScl, radius * 2.35f * triScl, ang + 90 * i);
					}
					
					ang *= -1.5f;
					for(int i : Mathf.signs){
						DrawFunc.tri(ex, ey, radius / 4f * triScl, radius * 1.85f * triScl, ang + 90 * i);
					}
					
					Draw.color(Color.white);
					Fill.circle(
							ex,
							ey,
							(radius + Mathf.absin(Time.time, 4f, radius / 4f)) * 0.785f  * scale
					);
					
					Draw.color(Color.black);
					Fill.circle(
						ex,
						ey,
						(radius + Mathf.absin(Time.time, 4f, radius / 4f)) * 0.7f  * scale
					);
					
//					Draw.z(z);
				}
			}
			
			engines.add(new PestEngine(0f, -engineOffset, 16f, -90f));
			engines.add(new PestEngine(0f, -115, 6f, -90f, 0.3f));
			engineLayer = Layer.effect + 0.005f;
			
			Weapon laser = new Weapon(name + "-laser"){{
				mirror = true;
				rotateSpeed = 2.15f;
				rotate = true;
				
				parts.add(new RegionPart("-shooter"){{
					layerOffset = 0.05f;
					
					moveY = 7.5f;
					progress = PartProgress.warmup;
				}}, new RegionPart("-top"){{
					layerOffset = 0.075f;
					outline = false;
				}});
				
				shootSound = NHSounds.laser5;
				reload = 60f;
				shootWarmupSpeed = 0.05f;
				shoot = new AdaptedShootHelix(){{
					shots = 5;
					mag = 1.15f;
					scl = 6f;
					shotDelay = 3.3f;
					offset = Mathf.PI2 * 12;
					
					firstShotDelay = 10f;
				}};
				
				shootY = 9;
				velocityRnd = 0.075f;
				
				bullet = new AccelBulletType(2.85f, 120f){{
					frontColor = Color.white;
					backColor = lightningColor = trailColor = hitColor = lightColor = NHColor.ancient;
					lifetime = 126f;
					knockback = 2f;
					ammoMultiplier = 8f;
					accelerateBegin = 0.1f;
					accelerateEnd = 0.85f;
					
					status = NHStatusEffects.entangled;
					statusDuration = 30f;
					
					despawnSound = hitSound = Sounds.dullExplosion;
					
					velocityBegin = 8f;
					velocityIncrease = -5f;
					
					homingDelay = 20f;
					homingPower = 0.05f;
					homingRange = 120f;
					
					despawnHit = pierceBuilding = true;
					hitShake = despawnShake = 5f;
					lightning = 1;
					lightningCone = 360;
					lightningLengthRand = 12;
					lightningLength = 4;
					width = 10f;
					height = 35f;
					pierceCap = 8;
					shrinkX = shrinkY = 0;
					
					lightningDamage = damage * 0.85f;
					
					hitEffect = NHFx.hitSparkLarge;
					despawnEffect = NHFx.square45_6_45;
					shootEffect = NHFx.shootCircleSmall(backColor);
					smokeEffect = NHFx.hugeSmokeGray;
					trailEffect = NHFx.trailToGray;
					
					trailLength = 15;
					trailWidth = 2f;
					drawSize = 300f;
				}};
			}};
			
			Weapon pdf = new PointDefenseWeapon(){{
				beamEffect = Fx.chainLightning;
				
				mirror = false;
				x = 0f;
				y = -engineOffset;
				reload = 5f;
				targetInterval = 10f;
				targetSwitchInterval = 8f;
				shootSound = NHSounds.gauss;
				
				bullet = new BulletType(){{
					shootEffect = Fx.none;
					hitEffect = NHFx.lightningHitSmall;
					maxRange = 200f;
					damage = 200f;
				}};
			}
				@Override
				protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
					if(!(mount.target instanceof Bullet)) return;
					
					Bullet target = (Bullet)mount.target;
					if(target.damage() > bullet.damage){
						target.damage(target.damage() - bullet.damage);
					}else{
						target.remove();
					}
					
					beamEffect.at(shootX, shootY, rotation, unit.team.color, new Vec2().set(target));
					bullet.shootEffect.at(shootX, shootY, rotation, unit.team.color);
					bullet.hitEffect.at(target.x, target.y, 12f, unit.team.color);
					shootSound.at(shootX, shootY, Mathf.random(0.9f, 1.1f));
					mount.recoil = 1f;
					mount.heat = 1f;
				}
			};
			
			weapons.add(copyAndMove(laser, 34.5f, 1.25f));
			weapons.add(copyAndMove(laser, 30f, -74.5f));
			weapons.add(new Weapon(name + "-cannon"){{
				x = 56;
				y = -48;
				shootY = 18f;
				
				rotate = true;
				rotateSpeed = 1.4f;
				
				shoot = new ShootPattern();
				shootSound = NHSounds.thermo;
				recoil = 3f;
				
				reload = 90f;
				
				bullet = new AccelBulletType(2.85f, 240f, "mine-bullet"){{
					frontColor = Color.white;
					backColor = lightningColor = trailColor = hitColor = lightColor = NHColor.ancient;
					lifetime = 95f;
					
					status = NHStatusEffects.entangled;
					statusDuration = 300f;
					
					accelerateBegin = 0.15f;
					accelerateEnd = 0.95f;
					
					despawnSound = hitSound = Sounds.titanExplosion;
					
					velocityBegin = 8f;
					velocityIncrease = -7.5f;
					
					collides = false;
					scaleLife = scaledSplashDamage = true;
					despawnHit = true;
					hitShake = despawnShake = 18f;
					lightning = 4;
					lightningCone = 360;
					lightningLengthRand = 12;
					lightningLength = 10;
					width = height = 30;
					shrinkX = shrinkY = 0;
					
					splashDamageRadius = 120f;
					splashDamage = 800f;
					
					lightningDamage = damage * 0.85f;
					
					hitEffect = NHFx.hitSparkLarge;
					despawnEffect = NHFx.square45_6_45;
					shootEffect = NHFx.shootCircleSmall(backColor);
					smokeEffect = NHFx.hugeSmokeGray;
					trailEffect = NHFx.trailToGray;
					
					trailLength = 15;
					trailWidth = 5f;
					drawSize = 300f;
					
					shootEffect = NHFx.instShoot(backColor, frontColor);
					smokeEffect = NHFx.lightningHitLarge;
					
					hitEffect = new Effect(90, e -> {
						Draw.color(backColor, frontColor, e.fout() * 0.7f);
						Fill.circle(e.x, e.y, e.fout() * height / 1.25f);
						Lines.stroke(e.fout() * 3f);
						Lines.circle(e.x, e.y, e.fin() * 80);
						Lines.stroke(e.fout() * 2f);
						Lines.circle(e.x, e.y, e.fin() * 50);
						Angles.randLenVectors(e.id, 35, 18 + 100 * e.fin(), (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 12 + 4));
						
						Draw.color(frontColor);
						Fill.circle(e.x, e.y, e.fout() * height / 1.75f);
					});
					despawnEffect = new OptionalMultiEffect(NHFx.hitSparkHuge, NHFx.instHit(backColor, 3, 120f));
					
					fragBullets = 3;
					fragBullet = new LaserBulletType(){{
						length = 460f;
						damage = 4060f;
						width = 45f;
						
						status = NHStatusEffects.entangled;
						statusDuration = 120f;
						
						lifetime = 65f;
						
						splashDamage = 800;
						splashDamageRadius = 120;
						hitShake = 18f;
						
						lightningSpacing = 35f;
						lightningLength = 8;
						lightningDelay = 1.1f;
						lightningLengthRand = 15;
						lightningDamage = 450;
						lightningAngleRand = 40f;
						scaledSplashDamage = largeHit = true;
						
						lightningColor = trailColor = hitColor = lightColor = Items.surgeAlloy.color.cpy().lerp(Pal.accent, 0.055f);
						
						despawnHit = false;
						hitEffect = new Effect(90, 500, e -> {
							Draw.color(backColor, frontColor, e.fout() * 0.7f);
							Fill.circle(e.x, e.y, e.fout() * height / 1.55f);
							Lines.stroke(e.fout() * 3f);
							Lines.circle(e.x, e.y, e.fin(Interp.pow3Out) * 80);
							Angles.randLenVectors(e.id, 18, 18 + 100 * e.fin(), (x, y) -> Fill.circle(e.x + x, e.y + y, e.fout() * 7f));
							
							Draw.color(frontColor);
							Fill.circle(e.x, e.y, e.fout() * height / 2f);
						});
						
						sideAngle = 15f;
						sideWidth = 0f;
						sideLength = 0f;
						colors = new Color[]{hitColor.cpy().a(0.2f), hitColor, Color.white};
					}
						
						@Override
						public void despawned(Bullet b){
							//							super.despawned(b);
						}
						
						@Override
						public void init(Bullet b){
							Vec2 p = new Vec2().set(NHFunc.collideBuildOnLength(b.team, b.x, b.y, length, b.rotation(), bu -> true));
							
							float resultLength = b.dst(p), rot = b.rotation();
							
							b.fdata = resultLength;
							laserEffect.at(b.x, b.y, rot, resultLength * 0.75f);
							
							Time.run(b.lifetime() * 0.2f, () -> {
								hit(b, p.getX(), p.getY());
							});
							
							if(lightningSpacing > 0){
								int idx = 0;
								for(float i = 0; i <= resultLength; i += lightningSpacing){
									float cx = b.x + Angles.trnsx(rot,  i),
											cy = b.y + Angles.trnsy(rot, i);
									
									int f = idx++;
									
									for(int s : Mathf.signs){
										Time.run(f * lightningDelay, () -> {
											if(b.isAdded() && b.type == this){
												Lightning.create(b, lightningColor,
														lightningDamage < 0 ? damage : lightningDamage,
														cx, cy, rot + 90*s + Mathf.range(lightningAngleRand),
														lightningLength + Mathf.random(lightningLengthRand));
											}
										});
									}
								}
							}
						}
						
						@Override
						public void draw(Bullet b){
							float realLength = b.fdata;
							
							float f = Mathf.curve(b.fin(), 0f, 0.2f);
							float baseLen = realLength * f;
							float cwidth = width;
							float compound = 1f;
							
							Tmp.v1.trns(b.rotation(), baseLen);
							
							for(Color color : colors){
								Draw.color(color);
								Lines.stroke((cwidth *= lengthFalloff) * b.fout());
								Lines.lineAngle(b.x, b.y, b.rotation(), baseLen, false);
								
								Fill.circle(Tmp.v1.x + b.x, Tmp.v1.y + b.y, Lines.getStroke() * 2.2f);
								Fill.circle(b.x, b.y, 1f * cwidth * b.fout());
								compound *= lengthFalloff;
							}
							Draw.reset();
							Drawf.light(b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, width * 1.4f * b.fout(), colors[0], 0.6f);
						}
					};
				}};
			}});
			weapons.add(new Weapon(){{
				y = -engineOffset;
				x = 0;
				
				shootSound = Sounds.beam;
				rotationLimit = 5f;
				rotateSpeed = 0.75f;
				rotate = true;
				mirror = false;
				continuous = true;
				predictTarget = false;
				
				reload = 480f;
				shootCone = 5f;
				
				bullet = new ContinuousLaserBulletType(){{
					length = 800f;
					lightningColor = hitColor = lightColor = NHColor.ancient;
					
					lifetime = 120f;
					width = 8f;
					
					status = NHStatusEffects.entangled;
					statusDuration = 30f;
					
					damage = 350;
					lightningDamage = 120;
					lightning = 2;
					lightningLength = lightningLengthRand = 10;
					
					hitEffect = NHFx.square45_8_45;
					shootEffect = Fx.none;
					smokeEffect = NHFx.hugeSmokeGray;
					
					largeHit = true;
					backLength += 12f;
					frontLength += 20f;
					
					colors = new Color[]{Color.white.cpy().a(0), Color.white, Color.white};
				}
					
					@Override
					public void init(Bullet b){
						super.init(b);
						
						NHFx.lightningHitLarge.at(b.x, b.y, b.team.color);
						Sounds.laserblast.at(b);
					}
					
					@Override
					public void hit(Bullet b, float x, float y){
						hitEffect.at(x, y, b.rotation(), b.team.color);
						hitSound.at(x, y, hitSoundPitch, hitSoundVolume);
						
						Effect.shake(hitShake, hitShake, b);
						
						if(fragOnHit){
							createFrags(b, x, y);
						}
						createPuddles(b, x, y);
						createIncend(b, x, y);
						createUnits(b, x, y);
						
						if(suppressionRange > 0){
							//bullets are pooled, require separate Vec2 instance
							Damage.applySuppression(b.team, b.x, b.y, suppressionRange, suppressionDuration, 0f, suppressionEffectChance, new Vec2(b.x, b.y));
						}
						
						createSplashDamage(b, x, y);
						
						for(int i = 0; i < lightning; i++){
							Lightning.create(b, b.team.color, lightningDamage < 0 ? damage : lightningDamage, x, y, b.rotation() + Mathf.range(lightningCone/2) + lightningAngle, lightningLength + Mathf.random(lightningLengthRand));
						}
						
						if(!Vars.headless && Mathf.chanceDelta(0.1))PosLightning.createEffect(b, Tmp.v1.set(x, y), b.team.color, 1, 2.2f);
						if(b.owner instanceof Healthc){
							Healthc h = (Healthc)b.owner;
							h.heal(80);
						}
					}
					
					public void applyDamage(Bullet b){
						Damage.collideLine(b, b.team, Fx.none, b.x, b.y, b.rotation(), currentLength(b), largeHit, laserAbsorb, pierceCap);
					}
				
					@Override
					public void draw(Bullet b){
						float realLength = Damage.findLaserLength(b, length);
						float fout = Mathf.clamp(b.time > b.lifetime - fadeTime ? 1f - (b.time - (lifetime - fadeTime)) / fadeTime : 1f);
						float baseLen = realLength * fout;
						float rot = b.rotation();
						
						for(int i = 0; i < colors.length; i++){
							Draw.color(Tmp.c1.set(b.team.color).lerp(colors[i], (float)i / (colors.length + 1f)).mul(1f + Mathf.absin(Time.time, 1f, 0.1f)));
							
							float colorFin = i / (float)(colors.length - 1);
							float baseStroke = Mathf.lerp(strokeFrom, strokeTo, colorFin);
							float stroke = (width + Mathf.absin(Time.time, oscScl, oscMag)) * fout * baseStroke;
							float ellipseLenScl = Mathf.lerp(1 - i / (float)(colors.length), 1f, pointyScaling);
							
							Lines.stroke(stroke);
							Lines.lineAngle(b.x, b.y, rot, baseLen - frontLength, false);
							
							//back ellipse
							Drawf.flameFront(b.x, b.y, divisions, rot + 180f, backLength, stroke / 2f);
							
							//front ellipse
							Tmp.v1.trnsExact(rot, baseLen - frontLength);
							Drawf.flameFront(b.x + Tmp.v1.x, b.y + Tmp.v1.y, divisions, rot, frontLength * ellipseLenScl, stroke / 2f);
						}
						
						Tmp.v1.trns(b.rotation(), baseLen * 1.1f);
						
						Drawf.light(b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, lightStroke, b.team.color, 0.7f);
						Draw.reset();
					}
				};
			}});
			
			weapons.add(copyAndMove(ancientSecTurret, 42f, 55f));
			weapons.add(copyAndMove(ancientSecTurret, 51.5f, -80.5f));
			weapons.add(copyAndMove(ancientSecTurret, 37f, -95.75f));
			
			weapons.add(pdf);
			weapons.add(pdf.copy());
			weapons.add(pdf.copy());
			
			lowAltitude = true;
			itemCapacity = 500;
			health = 220000.0F;
			speed = 0.45F;
			accel = 0.04F;
			drag = 0.025F;
			flying = true;
			hitSize = 130.0F;
			armor = 150.0F;

			rotateSpeed = 0.35F;
			buildSpeed = 10f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		hurricane = new NHUnitType("hurricane"){{
			drawShields = false;
			outlineColor = OColor;
			outlineRadius += 1;
				constructor = EntityMapping.map(3);
				
				weapons.add(copyAndMove(pointDefenceWeaponC, 20, -31));
				weapons.add(copyAndMove(pointDefenceWeaponC, 16, -22));

				weapons.add(
					new Weapon(){{
						rotate = false;
						mirror = true;
						shoot = new ShootPattern(){{
							shots = 8;
							shotDelay = 10f;
						}};
						
						reload = 150f;
						x = 27.75f;
						y = -4f;
						
						shootSound = NHSounds.launch;
						bullet = new TextureMissileType(200f, NewHorizon.name("ann-missile")){{
							velocityBegin = 2f;
							velocityIncrease = 12f;
							accelInterp = Interp.pow2In;
							
							lightningDamage = damage * 0.6f;
							lightningLength = lightningLengthRand = 8;
							
							splashDamage = damage;
							scaledSplashDamage = true;
							splashDamageRadius = 30f;
							incendAmount = 2;
							incendChance = 0.08f;
							incendSpread = 24f;
							makeFire = true;
							lifetime = 70f;
							trailEffect = NHFx.trailToGray;
							trailParam = 3f;
							trailChance = 0.3f;
							trailLength = 18;
							trailWidth = 1.6f;
							
							homingPower = 0.075f;
							homingRange = 200f;
							homingDelay = 10f;
							
							width = 5;
							height = 18f;
							
							trailColor = backColor = hitColor = lightColor = lightningColor = NHColor.lightSkyBack;
							frontColor = NHColor.lightSkyFront;
							
							smokeEffect = Fx.none;
							shootEffect = Fx.none;
							hitEffect = NHFx.blast(backColor, splashDamageRadius);
							despawnEffect = NHFx.hitSparkLarge;
							
							hitShake = despawnShake = 3f;
							despawnSound = hitSound = Sounds.explosion;
						}};
					}},
					new Weapon(){{
						predictTarget = false;
						mirror = false;
						rotate = true;
						rotationLimit = 5f;
						rotateSpeed = 0.5f;
						continuous = true;
						alternate = false;
						shake = 5f;
						shootY = 47f;
						reload = 220f;
						shoot = new ShootPattern(){{
							shots = 1;
						}};
						
						x = 0;
						y = 25f;
						ejectEffect = Fx.none;
						recoil = 4.4f;
						bullet = NHBullets.atomSeparator.copy();
						bullet.lifetime = 300f;
						bullet.damage = 200f;
						bullet.shootEffect = NHFx.lightningHitLarge(NHColor.lightSkyBack);
						((ContinuousFlameBulletType)bullet).lengthInterp = NHInterp.laser;
						shootSound = Sounds.beam;
						shootStatus = StatusEffects.slow;
						shootStatusDuration = bullet.lifetime + 40f;
					}},
					new Weapon(NewHorizon.name("swepter")){{
						mirror = false;
						top = true;
						rotate = true;
						rotationLimit = 240f;
						alternate = false;
						shake = 5f;
						shootY = 17f;
						reload = 300f;
						rotateSpeed = 2.15f;
						
						shoot = new ShootPattern(){{
							shots = 1;
						}};
						
						y = -40f;
						x = 0f;
						inaccuracy = 3.0F;
						ejectEffect = Fx.none;
						recoil = 5.4f;
						bullet = new LightningLinkerBulletType(2.5f, 250){{
							
							range = 340f;
							
							trailWidth = 8f;
							trailLength = 40;
							
							backColor = trailColor = lightColor = lightningColor = NHColor.lightSkyBack;
							frontColor = Color.white;
							randomGenerateRange = 280f;
							randomLightningNum = 5;
							linkRange = 280f;
							
							scaleLife = true;
							
							hitModifier = UltFire::create;
							
							size /= 1.5f;
							drag = 0.0065f;
							fragLifeMin = 0.125f;
							fragLifeMax = 0.45f;
							fragVelocityMax = 0.75f;
							fragVelocityMin = 0.25f;
							fragBullets = 13;
							fragBullet = NHBullets.basicSkyFrag;
							hitSound = Sounds.explosionbig;
							drawSize = 40;
							splashDamageRadius = 240;
							splashDamage = 80;
							lifetime = 300;
							despawnEffect = Fx.none;
							hitEffect = new Effect(50, e -> {
								color(NHColor.lightSkyBack);
								Fill.circle(e.x, e.y, e.fout() * 44);
								stroke(e.fout() * 3.2f);
								circle(e.x, e.y, e.fin() * 80);
								stroke(e.fout() * 2.5f);
								circle(e.x, e.y, e.fin() * 50);
								Angles.randLenVectors(e.id, 30, 18 + 80 * e.fin(), (x, y) -> {
									stroke(e.fout() * 3.2f);
									lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 14 + 5);
								});
								color(Color.white);
								Fill.circle(e.x, e.y, e.fout() * 30);
							});
							shootEffect = new Effect(30f, e -> {
								color(NHColor.lightSkyBack);
								Fill.circle(e.x, e.y, e.fout() * 32);
								color(Color.white);
								Fill.circle(e.x, e.y, e.fout() * 20);
							});
							smokeEffect = new Effect(40f, 100, e -> {
								color(NHColor.lightSkyBack);
								stroke(e.fout() * 3.7f);
								circle(e.x, e.y, e.fin() * 100 + 15);
								stroke(e.fout() * 2.5f);
								circle(e.x, e.y, e.fin() * 60 + 15);
								randLenVectors(e.id, 15, 7f + 60f * e.finpow(), (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 4f + e.fout() * 16f));
							});
						}
							
							@Override
							public void despawned(Bullet b){
								super.despawned(b);
								
								UltFire.createChance(b, splashDamageRadius / 2f, 0.5f);
							}
						};
						shootSound = Sounds.laserblast;
					}},
					new Weapon(NewHorizon.name("impulse")){{
						heatColor = NHColor.lightSkyBack;
						top = true;
						rotate = true;
						shootY = 8f;
						reload = 50f;
						x = 40f;
						y = -30f;
						rotateSpeed = 6f;
						
						inaccuracy = 6.0F;
						velocityRnd = 0.38f;
						alternate = false;
						ejectEffect = Fx.none;
						recoil = 1.7f;
						shootSound = Sounds.plasmaboom;
						
						bullet = new ChainBulletType(300f){{
							length = 360f;
							hitColor = lightColor = lightningColor = NHColor.lightSkyBack;
							shootEffect = NHFx.hitSparkLarge;
							hitEffect = NHFx.lightningHitSmall;
							smokeEffect = NHFx.hugeSmokeGray;
						}};
						
						shoot = new ShootPattern(){{
							shots = 3;
							shotDelay = 15f;
						}};
					}}
				);
			
				for(int i : Mathf.signs){
					engines.add(new UnitEngine(i * 44.25f, -49f, 7, -90 + 8 * i));
				}
				
				abilities.add(new RepairFieldAbility(800f, 160f, 240f){{
					healEffect = NHFx.healEffectSky;
					activeEffect = NHFx.activeEffectSky;
				}});
				
				lowAltitude = true;
				itemCapacity = 500;
				health = 72000.0F;
				speed = 1F;
				accel = 0.04F;
				drag = 0.025F;
				flying = true;
				hitSize = 100.0F;
				armor = 60.0F;
				engineOffset = 60.0F;
				engineSize = 20.0F;
				rotateSpeed = 0.75F;
				buildSpeed = 2.8f;
			}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		assaulter = new NHUnitType("assaulter") {
			{
				constructor = EntityMapping.map(3);
				itemCapacity = 10;
				hitSize = 16.0F;
				armor = 8.0F;
				health = 220.0F;
				speed = 3.0F;
				rotateSpeed = 2.75F;
				accel = 0.075F;
				drag = 0.035F;
				flying = true;
				engineOffset = 12.0F;
				engineSize = 3.0F;
				buildSpeed = 0.0F;
				abilities.add(new BoostAbility(false, 1.5F, 15.0F));
				range = maxRange = 148.0F;
				targetFlags = new BlockFlag[]{BlockFlag.turret, BlockFlag.factory, BlockFlag.reactor, BlockFlag.generator, BlockFlag.core, null};
				weapons.add(new Weapon() {
					{
						reload = 155;
						shoot = new ShootPattern(){{
							firstShotDelay = 120.0F;
						}};
						shootStatus = NHStatusEffects.stronghold;
						shootStatusDuration = 680.0F;
						x = y = 0.0F;
						shootY = 15.0F;
						continuous = parentizeEffects = true;
						mirror = alternate = false;
						shootCone = 5.0F;
						chargeSound = Sounds.lasercharge;
						shootSound = Sounds.laserblast;
						bullet = new LaserBulletType(200.0F) {
							{
								chargeEffect = new Effect(120.0F, e -> {
									Draw.color(NHColor.thurmixRed);
									float p = NHInterp.upThenFastDown.apply(e.fin());
									Lines.stroke(p * 2f);
									Lines.circle(e.x, e.y, 45 * p);
									Lines.spikes(e.x, e.y, (65 - e.fin() * 45) * p, (e.fin() * 26 + 3) * p, 4, 45 + e.rotation);
									
									Lines.stroke(e.finpow() * 3f);
									Lines.circle(e.x, e.y, 72 * e.fout(Interp.pow5Out));
									
									Fill.circle(e.x, e.y, 6 * e.fin());
									Draw.color(NHColor.thurmixRedLight);
									Fill.circle(e.x, e.y, 3.5f * e.fin());
								}).followParent(true);
								
								colors = new Color[]{NHColor.thurmixRed.cpy().mul(1.0F, 1.0F, 1.0F, 0.3F), NHColor.thurmixRed, Color.white};
								length = 180.0F;
								width = 18.0F;
								lengthFalloff = 0.6F;
								sideLength = 90.0F;
								sideWidth = 1.35F;
								sideAngle = 40.0F;
								lightningSpacing = 40.0F;
								lightningLength = 2;
								lightningDelay = 1.1F;
								lightningLengthRand = 10;
								lightningDamage = damage / 5.0F;
								lightningAngleRand = 40.0F;
								hitColor = lightningColor = NHColor.thurmixRed;
								smokeEffect = shootEffect = Fx.none;
								hitEffect = NHFx.laserHit(NHColor.thurmixRed);
								lifetime = 30.0F;
								status = NHStatusEffects.weak;
								statusDuration = 60.0F;
								killShooter = true;
								keepVelocity = false;
							}
							
							@Override
							public void init(Bullet b){
								super.init(b);
								
								if(killShooter && b.owner() instanceof Healthc){
									((Healthc) b.owner()).kill();
								}
							}
						};
					}
				});
			}
			
			public void createIcons(MultiPacker packer) {
				super.createIcons(packer);
				NHPixmap.createIcons(packer, this);
			}
		};
		
		rhino = new NHUnitType("rhino"){{
			outlineColor = OColor;
			immunities = ObjectSet.with(NHStatusEffects.ultFireBurn, NHStatusEffects.emp1, NHStatusEffects.emp2, StatusEffects.shocked, StatusEffects.burning, StatusEffects.melting, StatusEffects.electrified, StatusEffects.wet, StatusEffects.slow, StatusEffects.blasted);
			aiController = BuilderAI::new;
			defaultCommand = UnitCommand.rebuildCommand;
			constructor = EntityMapping.map(3);
			abilities.add(new BoostAbility());
			weapons.add(new RepairBeamWeapon("point-defense-mount"){{
				y = -8.5f;
				x = 0;
				shootY = 4f;
				mirror = false;
				beamWidth = 0.7f;
				repairSpeed = 1f;

				bullet = new BulletType(){{
					maxRange = 120f;
				}};
			}});
			armor = 12;
			buildBeamOffset = 6f;
			buildSpeed = 5f;
			hitSize = 20f;
			flying = true;
			drag = 0.06F;
			accel = 0.12F;
			itemCapacity = 200;
			speed = 1F;
			health = 3000.0F;
			engineSize = 3.4F;
			engineOffset = 10.5f;
			isEnemy = false;
			lowAltitude = true;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		naxos = new NHUnitType("naxos"){{
			outlineColor = OColor;
			constructor = EntityMapping.map(3);
			health = 8500.0F;
			speed = 3f;
			accel = 0.75F;
			drag = 0.015F;
			flying = true;
			targetAir = true;
			targetGround = false;
			circleTarget = true;
			hitSize = 16.0F;
			armor = 40.0F;
			engineOffset = 12.5f;
			engineSize = 5.0F;
			rotateSpeed = 4.75f;
			buildSpeed = 1.25f;
			lowAltitude = false;

			aiController = InterceptorAI::new;
			
			for(int i : Mathf.signs){
				engines.add(new UnitEngine(i * 14.25f, -14.5f, 3, -90 + i * 15));
			}
			
			abilities.add(new BoostAbility(2.25f, 160f));

			weapons.add(
					new Weapon(NewHorizon.name("impulse-side")){{
						mirror = alternate = true;
						top = rotate = false;
						reload = 45f;

						inaccuracy = 5f;

						x = -10.5f;
						y = -2f;
						shootY = 6f;
						shootX = 1;

						shootCone = 30f;

						shoot = new ShootPattern(){{
							shots = 3;
							shotDelay = 8f;
						}};
						
						shootSound = NHSounds.laser5;

						bullet = new TrailFadeBulletType(7f, 200f, "missile-large"){{
							trailLength = 20;
							trailWidth = 2.5f;
							trailColor = lightColor = lightningColor = backColor = hitColor = NHColor.lightSkyBack;
							frontColor = NHColor.lightSkyFront;
							
							tracers = 1;
							tracerUpdateSpacing *= 2.25f;
							tracerRandX *= 0.75f;
							
							hitBlinkTrail = false;
							
							width = 10f;
							height = 30f;

							weaveScale = 7f;
							weaveMag = 0.8f;

							homingDelay = 8f;
							homingPower = 0.7f;
							homingRange = 200f;

							splashDamageRadius = 60f;
							splashDamage = damage / 2;

							shootEffect = NHFx.shootCircleSmall(backColor);
							smokeEffect = Fx.shootBigSmoke;
							hitEffect = NHFx.blast(backColor, splashDamageRadius);
							despawnEffect = NHFx.hitSparkLarge;
							despawnShake = hitShake = 5f;

							collidesAir = collides = true;
							collidesGround = collidesTiles =  false;
						}};
					}},
					new Weapon(){{
						reload = 180f;
						shootSound = Sounds.beam;
						x = 0;
						continuous = true;
						top = alternate = rotate = mirror = false;
						minShootVelocity = 2f;

						shootStatus = NHStatusEffects.invincible;
						shootStatusDuration = 360f;

						bullet = new BulletType(){{
							impact = true;
							keepVelocity = false;
							collides = false;
							pierce = true;
							hittable = false;
							absorbable = false;

							pierceArmor = true;
							
							collidesAir = true;
							collidesGround = collidesTiles = false;

							damage = 100f;
							lightning = 1;
							lightningDamage = damage / 4f;
							lightningLength = 10;
							lightningLengthRand = 15;

							knockback = 30f;

							lifetime = 360f;

							status = StatusEffects.melting;
							statusDuration = 60f;
							maxRange = 80f;
							speed = 0.0001f;

							lightColor = lightningColor = trailColor = hitColor = NHColor.lightSkyBack;
							hitEffect = NHFx.square(hitColor, 30f, 3, 80f, 5f);
							despawnEffect = Fx.none;
							shootEffect = NHFx.instShoot(hitColor, NHColor.lightSkyFront);
							smokeEffect = NHFx.square(hitColor, 45f, 5, 60f, 5f);
						}

							@Override
							public float continuousDamage(){
								return damage / 5f * 60f;
							}

							@Override
							public float estimateDPS(){
								//assume firing duration is about 100 by default, may not be accurate there's no way of knowing in this method
								//assume it pierces 3 blocks/units
								return damage * 100f / 5f * 3f;
							}

							@Override
							public void hit(Bullet b, float x, float y){
								super.hit(b, x, y);

								if(b.owner instanceof Healthc)((Healthc)b.owner).heal(b.damage / 3);
							}

							@Override
							public void update(Bullet b){

								//damage every 5 ticks
								if(b.timer(1, 5f)){
									Damage.collideLine(b, b.team, hitEffect, b.x, b.y, b.rotation(), maxRange, true, false);
								}

								if(shake > 0){
									Effect.shake(shake, shake, b);
								}
							}

							@Override
							public void draw(Bullet b){
								float f = Mathf.curve(b.fin(), 0, 0.015f) * Mathf.curve(b.fout(), 0, 0.025f);
								float sine = 1 + Mathf.absin(0.7f, 0.075f);
								float stroke = 6f;
								float offset = 8f;
								float rot = b.rotation();
								Draw.color(hitColor);
								Tmp.v1.trns(rot, 0, stroke).scl(f * sine);
								Tmp.v2.trns(rot, 0, stroke + stroke).scl(f * sine);
								Tmp.v3.trns(rot, maxRange).scl(f);
								for(int i : Mathf.signs){
									Fill.tri(
											b.x + Tmp.v1.x * i, b.y + Tmp.v1.y * i,
											b.x + Tmp.v2.x * i, b.y + Tmp.v2.y * i,
											b.x + Tmp.v3.x, b.y + Tmp.v3.y
									);
								}
								Draw.reset();
							}
						};
					}}
			);

			targetFlags = new BlockFlag[]{null};

			buildBeamOffset = 15f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		annihilation = new NHUnitType("annihilation"){{
			outlineColor = OColor;
			drawShields = false;
			weapons.add(
				new Weapon(NewHorizon.name("large-launcher")){{
					top = false;
					rotate = false;
					alternate = true;
					shake = 3.5f;
					shootY = 16f;
					x = 20f;
					recoil = 5.4f;
					predictTarget = false;
					shootCone = 30f;
					reload = 40f;
					shoot = new ShootSpread(){{
						spread = 1f;
						shots = 2;
						shotDelay = 7f;
					}};
					
					inaccuracy = 4.0F;
					ejectEffect = Fx.none;
					bullet = new ShrapnelBulletType(){{
						width -= 2;
						hitLarge = true;
						length = 280;
						damage = 200.0F;
						status = NHStatusEffects.ultFireBurn;
						statusDuration = 60f;
						fromColor = NHColor.lightSkyFront;
						toColor = NHColor.lightSkyBack;
						shootEffect = NHFx.lightningHitSmall(NHColor.lightSkyBack);
						smokeEffect = new MultiEffect(NHFx.lightSkyCircleSplash, new Effect(lifetime + 10f, b -> {
							Draw.color(fromColor, toColor, b.fin());
							Fill.circle(b.x, b.y, (width / 1.75f) * b.fout());
						}));
					}};
					shootSound = Sounds.shotgun;
				}}, new Weapon(){{
					mirror = false;
					rotate = true;
					alternate = true;
					rotateSpeed = 25f;
					x = 0;
					y = 8f;
					recoil = 2.7f;
					shootY = 7f;
					shootCone = 40f;
					reload = 180f;
					shoot = new ShootPattern(){{
						shots = 5;
						shotDelay = 16f;
					}};
					inaccuracy = 5.0F;
					ejectEffect = Fx.none;
					bullet = NHBullets.annMissile;
					shootSound = NHSounds.launch;
				}}
			);
			abilities.add(new ForceFieldAbility(64.0F, 1.25F, 3000.0F, 1200.0F));
			engineOffset = 15.0F;
			engineSize = 6.5F;
			speed = 0.275f;
			hitSize = 33f;
			health = 22000f;
			buildSpeed = 2.8f;
			armor = 15f;
			rotateSpeed = 1.8f;
			singleTarget = false;
			fallSpeed = 0.016f;
			mechStepParticles = true;
			stepShake = 0.5f;
			canBoost = true;
			mechLandShake = 6f;
			boostMultiplier = 3.5f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this); NHPixmap.outlineLegs(packer, this);}
		};
		
		sharp = new NHUnitType("sharp"){{
			outlineColor = OColor;
			constructor = EntityMapping.map(3);

			itemCapacity = 15;
			health = 140;
			armor = 1;
			engineOffset = 10F;
			engineSize = 2.8f;
			speed = 1.5f;
			flying = true;
			accel = 0.08F;
			drag = 0.02f;
			baseRotateSpeed = 1.5f;
			rotateSpeed = 2.5f;
			hitSize = 10f;
			singleTarget = true;

			weapons.add(new Weapon(){{
				top = false;
				rotate = false;
				alternate = false;
				mirror = false;
				x = 0f;
				y = 0f;
				reload = 30f;
				shoot = new ShootHelix(){{
					shots = 4;
					shotDelay = 4f;
				}};
				inaccuracy = 5f;
				ejectEffect = Fx.none;
				velocityRnd = 0.125f;
				shake = 2f;
				maxRange = 140f;
				bullet = new BasicBulletType(3.5f, 6f){{
					trailWidth = 1f;
					trailLength = 10;
					drawSize = 200f;

					homingPower = 0.1f;
					homingRange = 120f;
					width = 5f;
					height = 25f;
					keepVelocity = true;
					knockback = 0.75f;
					trailColor = backColor = lightColor = lightningColor = hitColor = NHColor.lightSkyBack;
					frontColor = backColor.cpy().lerp(Color.white, 0.45f);
					trailChance = 0.1f;
					trailParam = 1f;
					trailEffect = NHFx.trailToGray;
					despawnEffect = NHFx.square(backColor, 18f, 2, 12f, 2);
					hitEffect = NHFx.lightningHitSmall(backColor);
					shootEffect = NHFx.shootLineSmall(backColor);
					smokeEffect = Fx.shootBigSmoke2;
				}};
				shootSound = NHSounds.thermoShoot;
			}});
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};

		branch = new NHUnitType("branch"){{
			outlineColor = OColor;
			constructor = EntityMapping.map(3);
			weapons.add(new Weapon(){{
				top = false;
				rotate = true;
				alternate = true;
				mirror = false;
				shoot = new ShootPattern(){{
					shotDelay = 3f;
					shots = 5;
				}};
				x = 0f;
				y = -10f;
				reload = 30f;
				inaccuracy = 4f;
				ejectEffect = Fx.none;
				bullet = new FlakBulletType(2.55f, 15){{
					collidesGround = true;
					sprite = NHBullets.CIRCLE_BOLT;

					trailLength = 15;
					trailWidth = 3f;

					weaveMag = 4f;
					weaveScale = 4f;

					splashDamageRadius = 20f;
					explodeRange = splashDamageRadius / 1.5f;
					splashDamage = damage;

					homingDelay = 5f;
					homingPower = 0.005f;
					homingRange = 80f;

					lifetime = 60f;
					shrinkX = shrinkY = 0;
					backColor = lightningColor = hitColor = lightColor = trailColor = NHColor.lightSkyBack;
					frontColor = backColor.cpy().lerp(Color.white, 0.55f);
					width = height = 8f;
					smokeEffect = Fx.shootBigSmoke;
					shootEffect = NHFx.shootCircleSmall(backColor);
					hitEffect = NHFx.lightningHitSmall(backColor);
					despawnEffect = NHFx.shootCircleSmall(backColor);
				}};
				shootSound = Sounds.missileSmall;
			}});
			engineOffset = 9.0F;
			engineSize = 3f;
			speed = 2.4f;
			accel = 0.06F;
			drag = 0.035F;
			hitSize = 14f;
			health = 460f;
			buildSpeed = 0.5f;
			baseRotateSpeed = 1.5f;
			rotateSpeed = 2.5f;
			armor = 3.5f;
			flying = true;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};

		warper = new NHUnitType("warper"){{
			outlineColor = OColor;
			constructor = EntityMapping.map(3);
			weapons.add(new Weapon(){{
				top = false;
				rotate = true;
				alternate = true;
				mirror = false;
				x = 0f;
				y = -10f;
				reload = 6f;
				inaccuracy = 3f;
				ejectEffect = Fx.none;
				bullet = NHBullets.warperBullet;
				shootSound = NHSounds.blaster;
			}});
			abilities.add(new MoveLightningAbility(10, 16, 0.2f, 12, 4, 6, NHColor.lightSkyBack));
			targetAir = false;
			maxRange = 200;
			engineOffset = 14.0F;
			engineSize = 4f;
			speed = 5f;
			accel = 0.04F;
			drag = 0.0075F;
			circleTarget = true;
			hitSize = 14f;
			health = 1000f;
			buildSpeed = 0.8f;
			baseRotateSpeed = 1.5f;
			rotateSpeed = 2.5f;
			armor = 3.5f;
			flying = true;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		origin = new NHUnitType("origin"){{
			outlineColor = OColor;
			weapons.add(
				new Weapon(NewHorizon.name("origin-weapon")){{
					mirror = true;
					this.top = false;
					
					rotate = true;
					rotationLimit = 15f;
					
					x = 5f;
					y = -1f;
					shootY = 6f;
					reload = 15f;
					shoot = new ShootSpread(){{
						shots = 3;
						spread = 3;
					}};
					inaccuracy = 4f;
					velocityRnd = 0.15f;
					shootSound = NHSounds.scatter;
					shake = 0.75f;
					bullet = new BasicBulletType(4f, 7f){{
						width = 5f;
						height = 25f;
						backColor = lightningColor = lightColor = hitColor = NHColor.lightSkyBack;
						frontColor = backColor.cpy().lerp(Color.white, 0.45f);
						shootEffect = NHFx.shootLineSmall(backColor);
						despawnEffect = NHFx.square(hitColor, 16f, 2, 12, 2f);
						hitEffect = NHFx.lightningHitSmall(backColor);
						smokeEffect = Fx.shootBigSmoke2;
						lifetime = 45f;
					}};
				}}
			);
			speed = 0.6F;
			hitSize = 8.0F;
			health = 160.0F;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this); NHPixmap.outlineLegs(packer, this);}
		};

		thynomo = new NHUnitType("thynomo"){{
			outlineColor = OColor;
			weapons.add(
				new Weapon(NewHorizon.name("thynomo-weapon")){{
					mirror = true;
					top = false;
					
					rotate = true;
					rotationLimit = 15f;
					
					x = 8f;
					y = 1f;
					shootY = 9.5f;
					reload = 90f;
					shootCone = 25f;
					shootStatus = StatusEffects.slow;
					shootStatusDuration = 90f;
					continuous = true;
					shootSound = Sounds.beam;
					bullet = new ContinuousLaserBulletType(18f){{
						length = 120f;
						width = 2.55f;

						incendChance = 0.025F;
						incendSpread = 5.0F;
						incendAmount = 1;
						
						shake = 3;
						colors = new Color[]{NHColor.lightSkyFront.cpy().mul(0.8f, 0.85f, 0.9f, 0.2f), NHColor.lightSkyBack.cpy().mul(1f, 1f, 1f, 0.5f), NHColor.lightSkyBack, Color.white};
						oscScl = 0.4f;
						oscMag = 1.5f;
						lifetime = 90f;
						lightColor = hitColor = NHColor.lightSkyBack;
						hitEffect = NHFx.lightSkyCircleSplash;
						shootEffect = NHFx.square(hitColor, 22f, 4, 16, 3f);
						smokeEffect = Fx.shootBigSmoke;
					}};
				}}
			);
			boostMultiplier = 2.0F;
			health = 650.0F;
			buildSpeed = 0.75F;
			rotateSpeed = 2.5f;
			canBoost = true;
			armor = 9.0F;
			mechLandShake = 2.0F;
			riseSpeed = 0.05F;
			mechFrontSway = 0.55F;
			speed = 0.4F;
			hitSize = 15f;
			engineOffset = 7.4F;
			engineSize = 4.25F;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this); NHPixmap.outlineLegs(packer, this);}
		};

		
		ghost = new NHUnitType("ghost"){{
			outlineColor = OColor;
			health = 1200;
			speed = 1.75f;
			drag = 0.18f;
			hitSize = 20f;
			armor = 12;
			accel = 0.1f;
			rotateSpeed = 2f;
			buildSpeed = 3f;

			weapons.add(
					copyAndMove(smallCannon, 12,-7),
					copyAndMove(smallCannon, 5,-1),
					new Weapon(NewHorizon.name("laser-cannon")){{
						top = rotate = true;
						rotateSpeed = 3f;
						x = 0;
						y = -11;

						recoil = 2f;
						mirror = false;
						reload = 60f;
						shootY = 5f;
						shootCone = 12f;
						shake = 8f;
						inaccuracy = 3f;
						shoot = new ShootPattern();
						predictTarget = true;

						shootSound = Sounds.laser;

						bullet = new BasicBulletType(2f, 90, "mine-bullet"){{
							scaleLife = true;
							keepVelocity = false;

							trailLength = 22;
							trailWidth = 4f;
							drawSize = 120f;
							recoil = 1.5f;

							trailChance = 0.1f;
							trailParam = 4f;
							trailEffect = NHFx.trailToGray;

							spin = 3f;
							shrinkX = shrinkY = 0.15f;
							height = width = 25f;
							lifetime = 160f;

							status = StatusEffects.blasted;

							backColor = trailColor = lightColor = lightningColor = hitColor = NHColor.lightSkyBack;
							frontColor = NHColor.lightSkyFront;

							splashDamage = damage / 3;
							splashDamageRadius = 24f;

							lightningLength = 2;
							lightningLengthRand = 4;
							lightningDamage = 10;

							hitSound = Sounds.explosion;
							hitShake = 8f;
							shootEffect = NHFx.shootCircleSmall(backColor);
							smokeEffect = Fx.shootSmallSmoke;
							despawnEffect = NHFx.lightningHitLarge(backColor);
							hitEffect = NHFx.hugeSmokeGray;
						}};
					}}
			);
			
			trailLength = 70;
			waveTrailX = 5f;
			waveTrailY = -13;
			trailScl = 1.65f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};

		zarkov = new NHUnitType("zarkov"){{
			outlineColor = OColor;
			weapons.add(
				copyAndMove(multipleLauncher, 8, -22),
				copyAndMove(multipleLauncher, 16, -8),
				copyAnd(smallCannon, weapon -> {
					weapon.x = 8.5f;
					weapon.y = 5.75f;
					weapon.autoTarget = true;
					weapon.controllable = false;
				})
			);
			health = 12000;
			speed = 1f;
			drag = 0.18f;
			hitSize = 42f;
			armor = 16f;
			accel = 0.1f;
			rotateSpeed = 1.6f;
			buildSpeed = 3f;

			trailLength = 70;
			waveTrailX = 7f;
			waveTrailY = -25f;
			trailScl = 2.6f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		tarlidor = new NHUnitType("tarlidor"){{
			outlineColor = OColor;
			abilities.add(new ShieldRegenFieldAbility(50.0F, 50F, 600.0F, 800.0F));
			weapons.add(
				new Weapon(NewHorizon.name("stiken")){{
					top = false;
					shake = 3f;
					shootY = 13f;
					reload = 50f;
					
					rotate = true;
					rotateSpeed = 0.85f;
					rotationLimit = 10f;
					
					shoot = new ShootPattern(){{
						shots = 2;
						shotDelay = 7f;
					}};
					
					parts.add(new RegionPart(){{
						under = true;
						name = NewHorizon.name("longinus-weapon-charger");
						x = 0f;
						y = -4f;
						moveY = -6f;
						progress = PartProgress.recoil;
						heatColor = Color.clear;
					}});
					
					x = 17.5f;
					inaccuracy = 3.0F;
					alternate = true;
					ejectEffect = Fx.none;
					recoil = 4.4f;
					bullet = new ShieldBreakerType(4.25f, 40, 650f){{
							drawSize = 500f;
							trailLength = 18;
							trailWidth = 3.5f;
							spin = 2.75f;
							despawnEffect = NHFx.square45_6_45;
							hitEffect = new Effect(45f, e -> {
								Draw.color(NHColor.lightSkyFront, NHColor.lightSkyBack, e.fin());
								Lines.stroke(2.5f * e.fout());
								DrawFunc.randLenVectors(e.id, e.fin(Interp.pow3Out), 3, 6, 21f, (x1, y1, fin, fout) -> Lines.square(e.x + x1, e.y + y1, 14 * Interp.pow3Out.apply(fin), 45));
							});
							lifetime = 50f;
							pierceCap = 8;
							width = 20f;
							height = 44f;
							lightColor = NHColor.lightSkyFront;
							backColor = lightningColor = hitColor = trailColor = NHColor.lightSkyBack;
							shootEffect = NHFx.shootLineSmall(backColor);

							frontColor = NHColor.lightSkyFront;
							lightning = 3;
							lightningDamage = damage / 4;
							lightningLength = 3;
							lightningLengthRand = 10;
							smokeEffect = Fx.shootBigSmoke2;
							hitShake = 4f;
							hitSound = Sounds.plasmaboom;
							shrinkX = shrinkY = 0.7f;
					}};
					shootSound = NHSounds.laser4;
				}}, new Weapon(NewHorizon.name("arc-blaster")){{
					top = true;
					rotate = true;
					shootY = 12f;
					reload = 45f;
					
					shoot = new ShootHelix(){{
						shots = 2;
						scl = 4f;
						shotDelay = 3.8f;
					}};
					
					
					rotateSpeed = 5f;
					inaccuracy = 6.0F;
					velocityRnd = 0.38f;
					
					x = 8f;
					alternate = false;
					ejectEffect = Fx.none;
					recoil = 1.7f;
					bullet = NHBullets.basicSkyFrag;
					shootSound = Sounds.plasmaboom;
				}}
			);

			engineOffset = 13.0F;
			engineSize = 6.5F;
			speed = 0.4f;
			hitSize = 20f;
			health = 7000f;
			buildSpeed = 1.8f;
			armor = 6f;
			rotateSpeed = 3.3f;
			fallSpeed = 0.016f;
			mechStepParticles = true;
			stepShake = 0.15f;
			canBoost = true;
			mechLandShake = 6f;
			boostMultiplier = 3.5f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this); NHPixmap.outlineLegs(packer, this);}
		};
		
		aliotiat = new NHUnitType("aliotiat"){{
			outlineColor = OColor;
			weapons.add(copyAndMoveAnd(posLiTurret, 10f, 3f, w -> {
				w.shoot = new ShootPattern();
				w.shoot.firstShotDelay = w.reload / 2;
			}), copyAndMove(posLiTurret, 10f, 3f), copyAndMove(posLiTurret, 10f, 3f), copyAndMove(posLiTurret, 6f, -2f));
			engineOffset = 10.0F;
			engineSize = 4.5F;
			speed = 0.35f;
			hitSize = 22f;
			health = 1200f;
			buildSpeed = 1.2f;
			armor = 5f;
			rotateSpeed = 2.8f;

			singleTarget = false;
			fallSpeed = 0.016f;
			mechStepParticles = true;
			stepShake = 0.15f;
			canBoost = true;
			mechLandShake = 6f;
			boostMultiplier = 3.5f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this); NHPixmap.outlineLegs(packer, this);}
		};
		
		gather = new NHUnitType("gather"){{
			outlineColor = OColor;
			aiController = MinerAI::new;
			defaultCommand = UnitCommand.mineCommand;
			constructor = EntityMapping.map(3);
			immunities = ObjectSet.with(NHStatusEffects.ultFireBurn, NHStatusEffects.emp1, NHStatusEffects.emp2, StatusEffects.shocked, StatusEffects.burning, StatusEffects.melting, StatusEffects.electrified, StatusEffects.wet, StatusEffects.slow, StatusEffects.blasted);
			weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
				y = -6.5f;
				x = 0;
				shootY = 6f;
				mirror = false;
				beamWidth = 0.7f;
				repairSpeed = 0.6f;

				bullet = new BulletType(){{
					maxRange = 120f;
				}};
			}});
			armor = 12;
			hitSize = 16f;
			flying = true;
			drag = 0.06F;
			accel = 0.12F;
			itemCapacity = 120;
			speed = 1.2F;
			health = 1200.0F;
			engineSize = 3.4F;
			engineOffset = 9.2F;
			range = 80.0F;
			isEnemy = false;
			mineTier = 6;
			mineSpeed = 10F;
			lowAltitude = true;
			
			mineItems.addAll(NHItems.zeta);
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this); NHPixmap.outlineLegs(packer, this);}
		};
		
		saviour = new NHUnitType("saviour"){{
			outlineColor = OColor;
			aiController = SniperAI::new;
			hitSize = 55f;
			armor = 36.0F;
			health = 34000.0F;
			speed = 0.9F;
			rotateSpeed = 0.75f;
			accel = 0.04F;
			drag = 0.035f;
			flying = true;
			engineOffset = 22.0F;
			engineSize = 12;
			buildSpeed = 8.0F;
			drawShields = false;
			lowAltitude = true;
			buildBeamOffset = 43.0F;
			payloadCapacity = (5 * 5) * tilePayload;

			targetFlags = new BlockFlag[]{BlockFlag.reactor, BlockFlag.generator, BlockFlag.battery, null};

			ammoType = new PowerAmmoType();
			
			for(int i : Mathf.signs){
				engines.add(new UnitEngine(i * 30.25f, -28.75f, 5, -90 + i * 45));
				engines.add(new UnitEngine(i * 48f, -13f, 4, -90 + i * 45));
			}
			
			immunities = ObjectSet.with(NHStatusEffects.ultFireBurn, NHStatusEffects.weak, NHStatusEffects.scannerDown, NHStatusEffects.emp1, NHStatusEffects.emp2, NHStatusEffects.emp3, StatusEffects.burning, StatusEffects.melting, NHStatusEffects.scrambler);

			weapons.add(
					new Weapon(){{
						x = shootX = shootY = 0;
						y = 22;

						reload = 180f;
						rotate = true;
						rotationLimit = 160f;
						
						mirror = false;
						shootCone = 15f;

						shake = 5;

						shootSound = Sounds.plasmadrop;

						bullet = new ShieldBreakerType(6, 30, "mine-bullet", 3000){{
							rangeOverride = 400;
							scaleLife = true;
							shootEffect = hitEffect = Fx.hitEmpSpark;
							smokeEffect = Fx.healWave;
							despawnEffect = NHFx.circleSplash(Pal.heal, 75f, 8, 68f, 7f);

							backColor = lightColor = trailColor = lightningColor = hitColor = Pal.heal;
							frontColor = Color.white;

							trailEffect = NHFx.trailSolid;
							trailParam = 4f;
							trailChance = 0.5f;

//							accelerateBegin = 0.15f;
//							accelerateEnd = 0.95f;
//							velocityBegin = 8f;
//							velocityIncrease = -8f;

							lifetime = 100f;

							width = height = 25f;
							shrinkX = shrinkY = 0f;
							spin = 4;

							trailWidth = 3.5f;
							trailLength = 18;

							pierceBuilding = true;
							pierceCap = 8;

							status = NHStatusEffects.scannerDown;
							statusDuration = 180f;
						}};
					}},
					new PointDefenseWeapon("point-defense-mount"){{
						mirror = true;
						x = 52;
						y = -5f;
						reload = 6f;
						targetInterval = 6f;
						targetSwitchInterval = 8f;

						bullet = new BulletType(){{
							color = Pal.heal;
							shootEffect = Fx.hitFlamePlasma;
							hitEffect = Fx.hitMeltHeal;
							maxRange = 240f;
							damage = 150f;
						}};
					}},
					new RepairBeamWeapon("repair-beam-weapon-center-large"){{
						x = 17f;
						y = -21;
						shootY = 6f;
						beamWidth = 0.8f;
						repairSpeed = 4f;

						bullet = new BulletType(){{
							maxRange = 160f;
						}};
					}},
					new Weapon(NewHorizon.name("saviour-cannon-smaller")){{
						top = true;
						rotate = true;
						rotationLimit = 60f;
						rotateSpeed = 1.5f;
						alternate = true;
						mirror = true;
						shake = 2f;
						shootY = 20f;
						x = 32f;
						y = -7;
						reload = 75f;
						recoil = 7f;
						shootSound = NHSounds.laser3;
						cooldownTime = 40f;

						bullet = NHBullets.saviourBullet;
					}}
			);

			abilities.add(new ForceFieldAbility(160f, 60f, 12000f, 900f), new RepairFieldAbility(1500, 180f, 400f), new HealFieldAbility(Pal.heal, 360f, 0, 22f, 400f, 0.15f){{
				effectRadius = 6f;
				sectors = 6;
				sectorRad = 0.065f;
			}}, new GravityTrapAbility(240));
		}

			@Override
			public void load(){
				super.load();
				shadowRegion = outlineRegion;
			}

			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		destruction = new NHUnitType("destruction"){{
			outlineColor = OColor;

			aiController = SniperAI::new;

			constructor = EntityMapping.map(3);
			
			weapons.add(copyAndMove(closeAATurret, 37, -18));
			weapons.add(copyAndMove(closeAATurret, 26, -8));
			
			weapons.addAll(
				new Weapon(){{
					alternate = mirror = false;
					top = rotate = true;
					x = 0;
					y = 0f;
					reload = 300f;
					
					shoot = new ShootPattern();
					
					ejectEffect = Fx.none;
					bullet = new AccelBulletType(0.05f, 50){
						{
							rangeOverride = 360f;
							buildingDamageMultiplier = 0.2f;
							width = height = 0;
							trailLength = 0;
							trailWidth = 0;
							lightning = 3;
							lightningLength = 2;
							lightningLengthRand = 18;
							homingDelay = 15f;
							homingPower = 10f;
							homingRange = 320f;
							splashDamage = lightningDamage = damage / 4;
							splashDamageRadius = 12f;
							backColor = lightColor = lightningColor = trailColor = NHColor.lightSkyBack;
							frontColor = Color.white;
							trailEffect = NHFx.polyCloud(backColor, 45, 10, 32, 4);
							trailChance = 0;
							pierce = pierceBuilding = true;
							velocityBegin = 1.25f;
							velocityIncrease = 8;
							accelerateBegin = 0.05f;
							accelerateEnd = 0.65f;
							lifetime = 100f;
							hitShake = 2;
							hitSound = Sounds.plasmaboom;
							hitEffect = NHFx.shootCircleSmall(backColor);
							despawnEffect = NHFx.lightningHitLarge(backColor);
							
							status = NHStatusEffects.scannerDown;
							statusDuration = 60f;
						}
						
						@Override
						public void update(Bullet b){
							if(Mathf.chanceDelta(0.45f))trailEffect.at(b.x, b.y, b.rotation());
							b.collided.clear();
							super.update(b);
						}
						
						@Override
						public void despawned(Bullet b){
							super.despawned(b);
							PosLightning.createRandomRange(b, b.team, b, splashDamageRadius * 30, lightColor, Mathf.chanceDelta(lightning / 10f), 0, 0, PosLightning.WIDTH, 2 + Mathf.random(1), lightning, hitPos -> {
								Damage.damage(b.team, hitPos.getX(), hitPos.getY(), splashDamageRadius, splashDamage * b.damageMultiplier(), collidesAir, collidesGround);
								NHFx.lightningHitLarge.at(hitPos.getX(), hitPos.getY(), lightningColor);
								NHFx.crossBlast.at(hitPos.getX(), hitPos.getY(), hitColor);
								for (int j = 0; j < lightning; j++) {
									Lightning.create(b, lightningColor, lightningDamage < 0.0F ? damage : lightningDamage, b.x, b.y, b.rotation() + Mathf.range(lightningCone / 2.0F) + lightningAngle, lightningLength + Mathf.random(lightningLengthRand));
								}
								hitSound.at(hitPos, Mathf.random(0.9f, 1.1f));
							});
							
							UltFire.createChance(b.x, b.y, splashDamageRadius * 6, 0.5f, b.team);
						}
						
						@Override
						public void hitEntity(Bullet b, Hitboxc other, float initialHealth){
							super.hitEntity(b, other, initialHealth);
							if(other instanceof Buildingc){
								b.time += b.lifetime() / 90f;
							}
						}
						
						@Override
						public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
							if(makeFire && build.team != b.team){
								UltFire.create(build.tile);
							}
							
							if(build.team != b.team && direct){
								hit(b);
							}
						}
					};
					shootSound = Sounds.plasmadrop;
				}},
				new Weapon(NewHorizon.name("arc-blaster")){{
					alternate = mirror = top = rotate = true;
					x = 10f;
					y = 4f;
					recoil = 3f;
					shootCone = 20f;
					reload = 120f;
					shoot = new ShootPattern();
					inaccuracy = 6f;
					shake = 5f;
					shootY = 5f;
					ejectEffect = Fx.none;
					predictTarget = false;
					bullet = new ChainBulletType(250){{
						hitColor = NHColor.lightSkyBack;
						hitEffect = NHFx.square(hitColor, 20f, 2, 16f, 3f);
						smokeEffect = Fx.shootBigSmoke;
						shootEffect = NHFx.shootLineSmall(hitColor);
					}};
					//bullet = NHBullets.longLaser;
					shootSound = Sounds.laser;
				}},
				new Weapon(){{
					x = shootX = shootY = 0;
					y = -2;
					xRand = 27f;

					rotate = mirror = alternate = false;

					bullet = new TextureMissileType(200f, NewHorizon.name("ann-missile")){{
						velocityBegin = 4f;
						velocityIncrease = 8f;
						
						absorbable = false;
						splashDamage = damage;
						splashDamageRadius = 20f;
						incendAmount = 2;
						incendChance = 0.08f;
						incendSpread = 24f;
						makeFire = true;
						lifetime += 12f;
						trailColor = NHColor.trail;
						trailEffect = NHFx.trailToGray;
						trailParam = 2f;
						trailChance = 0.2f;
						trailLength = 15;
						trailWidth = 1.2f;
						
						width = 5;
						height = 18f;
						
						backColor = hitColor = lightColor = lightningColor = NHColor.lightSkyBack;
						frontColor = NHColor.lightSkyFront;
						
						smokeEffect = Fx.none;
						shootEffect = Fx.none;
						hitEffect = NHFx.blast(backColor, splashDamageRadius * 0.75f);
						despawnEffect = NHFx.hitSparkLarge;
						
						collidesAir = false;
						collides = false;
						scaleLife = true;
						
						hitShake = despawnShake = 2f;
						despawnSound = hitSound = Sounds.explosion;
					}};

					shoot = new ShootPattern(){{
						shots = 20;
						shotDelay = 3.5f;
					}};
					reload = 300f;
					inaccuracy = 2f;
					velocityRnd = 0.1f;

					shake = 1.25f;
					shootSound = NHSounds.launch;
					shootCone = 8f;
				}}
			);
			
			armor = 15.0F;
			health = 15000.0F;
			speed = 0.45F;
			rotateSpeed = 1.0F;
			accel = 0.04F;
			drag = 0.018F;
			flying = true;
			engineOffset = 13F;
			engineSize = 11F;
			hitSize = 36.0F;
			buildSpeed = 1.25F;
			drawShields = false;
			lowAltitude = true;
			singleTarget = false;
			buildBeamOffset = 15F;
			ammoCapacity = 800;
			abilities.add(new RepairFieldAbility(500f, 160f, 240f){{
				healEffect = NHFx.healEffectSky;
				activeEffect = NHFx.activeEffectSky;
			}});

			targetFlags = new BlockFlag[]{BlockFlag.turret, BlockFlag.factory, BlockFlag.reactor, BlockFlag.generator, BlockFlag.core, null};
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		longinus = new NHUnitType("longinus"){{
			outlineColor = OColor;
			aiController = SniperAI::new;
			constructor = EntityMapping.map(3);
			lowAltitude = true;
			health = 10000.0F;
			speed = 0.45F;
			outlineRadius = 4;
			strafePenalty = 1f;
			accel = 0.02F;
			drag = 0.025F;
			flying = true;
			circleTarget = false;
			rotateMoveFirst = false;
			hitSize = 50.0F;
			armor = 15.0F;
			engineOffset = 46f;
			engineSize = 12.0F;
			rotateSpeed = 0.65f;
			buildSpeed = 3f;
			ammoType = new ItemAmmoType(NHItems.presstanium);
			
			targetFlags = new BlockFlag[]{BlockFlag.reactor, BlockFlag.turret, BlockFlag.generator, null};
			
			for(int i : Mathf.signs){
				engines.add(new UnitEngine(21.5f * i, -43.5f, 5, -90 + 45 * i));
//				engines.add(new UnitEngine(21.5f * i, -12.5f, 3f, 90 - 45 * i));
			}
			
			weapons.add(
				new Weapon(NewHorizon.name("longinus-weapon")){{
						shootY = 42;
						
						minWarmup = 0.95f;
						
						healColor = NHColor.thurmixRed;
						
						shoot = new ShootHelix(){{
							shots = 3;
							shotDelay = 18f;
							mag = 2.2f;
							scl = 2.2f;
						}};
						
						parts.add(new RegionPart("-ejector"){{
							progress = PartProgress.warmup.blend(PartProgress.recoil, 0.15f);
							under = turretShading = true;
							mirror = true;
							x = 15;
							y = 26.5f;
							
							moveX = 6f;
							moveY = -6f;
						}});
					
						parts.add(new RegionPart("-ejector"){{
							progress = PartProgress.warmup.blend(PartProgress.recoil, 0.15f);
							under = turretShading = true;
							mirror = true;
							x = 17;
							y = 14.5f;
							
							moveX = 6f;
							moveY = -6f;
						}});
					
						parts.add(new ShapePart(){{
							progress = PartProgress.smoothReload.inv().delay(0.65f).curve(Interp.pow3Out);
							y = shootY - 6f;
							sides = 4;
							color = NHColor.lightSkyBack;
							colorTo = NHColor.lightSkyMiddle;
							rotateSpeed = 2f;
							hollow = true;
							stroke = 0.0F;
							strokeTo = 1.5F;
							radius = 2.0F;
							radiusTo = 8f;
							moveY = 11f;
							layer = Layer.effect;
						}});
					
						parts.add(new ShapePart(){{
							progress = PartProgress.smoothReload.inv().delay(0.65f).curve(Interp.pow3Out);
							y = shootY - 6f;
							sides = 4;
							color = NHColor.lightSkyBack;
							colorTo = NHColor.lightSkyMiddle;
							rotateSpeed = -1f;
							hollow = true;
							stroke = 0.0F;
							strokeTo = 1.25F;
							radius = 4.0F;
							radiusTo = 10f;
							moveY = 11f;
							layer = Layer.effect;
						}});
					
/*
						for(int s : Mathf.signs){
							parts.add(new HaloPart(){{
								tri = true;
								progress = PartProgress.warmup;
								y = 7f;
								x = 3f * s;
								mirror = false;
								moveX = 7f * s;
								moveY = -2f;
								shapeMoveRot = -45f * s;
								shapes = 1;
								color = NHColor.lightSkyBack;
								colorTo = NHColor.lightSkyMiddle;
								shapeRotation = 90 * s;
								radius = -0.1f;
								radiusTo = 3f;
								triLength = -0.1f;
								triLengthTo = 3;
								layer = Layer.effect;
							}});
							
							parts.add(new HaloPart(){{
								tri = true;
								progress = PartProgress.warmup;
								y = 7f;
								x = 3f * s;
								mirror = false;
								moveX = 7f * s;
								moveY = -2f;
								shapeMoveRot = -45f * s;
								shapes = 1;
								color = NHColor.lightSkyBack;
								colorTo = NHColor.lightSkyMiddle;
								shapeRotation = -90 * s;
								radius = -0.1f;
								radiusTo = 3f;
								triLength = -0.1f;
								triLengthTo = 17;
								layer = Layer.effect;
							}});
						}
*/
						
						
						parts.add(new RegionPart("-panel"){{
							progress = PartProgress.warmup;
							outline = false;
							mirror = true;
							x = 9.25f;
							y = 20.5f;
							
							moveX = -1f;
							moveY = -1f;
						}});
						
						parts.add(new RegionPart("-main-charger"){{
							progress = PartProgress.warmup.blend(PartProgress.recoil.inv().curve(Interp.pow3Out), 0.15f);
							moves.add(new PartMove(PartProgress.recoil, 0, 8, 0));
							under = turretShading = true;
							layerOffset = -0.005f;
							heatLayerOffset = -0.005f;
							heatColor = Pal.sap;
							mirror = true;
							x = 16;
							y = -18;

							moveX = 10.75f;
							moveY = -2;
							moveRot = -45f;
						}});
						
						parts.add(new RegionPart("-charger"){{
							under = turretShading = true;
							heatLayerOffset = -0.005f;
							mirror = true;
							x = 10;
							y = 45.5f;
							
							moveX = -6f;
							moveY = 0;
						}});
						
						x = 0;
						y = -9f;
						recoil = 10;
						reload = 420f;
						cooldownTime = 150f;
						rotationLimit = 10f;
						shake = 12f;
						rotateSpeed = 0.55f;
						rotate = false;
						
						shootCone = 3f;
						
						top = false;
						mirror = false;
						shootSound = NHSounds.railGunBlast;
						soundPitchMax = 1.1f;
						soundPitchMin = 0.9f;
						
						layerOffset = -0.0005f;
						
						bullet = new TrailFadeBulletType(25f, 500f){{
							recoil = 0.095f;
							lifetime = 40f;
							trailLength = 200;
							trailWidth = 2F;
							tracers = 1;
							keepVelocity = false;
							
							tracerSpacing = 10f;
							tracerUpdateSpacing *= 1.25f;
							
							trailColor = hitColor = backColor = lightColor = lightningColor = NHColor.lightSkyBack;
							frontColor = NHColor.lightSkyFront;
							width = 10f;
							height = 40f;
							
							hitSound = Sounds.plasmaboom;
							despawnShake = hitShake = 18f;
							
							pierceArmor = true;
							
//							pierceArmor = pierceBuilding = true;
//							pierceCap = 4;
							
							lightning = 3;
							lightningLength = 6;
							lightningLengthRand = 18;
							lightningDamage = 150;
							
							smokeEffect = NHFx.square(hitColor, 80f, 8, 48f, 6f);
							shootEffect = NHFx.instShoot(backColor, frontColor);
							despawnEffect = NHFx.lightningHitLarge;
							hitEffect = new MultiEffect(NHFx.hitSpark(backColor, 75f, 24, 90f, 2f, 12f), NHFx.square45_6_45, NHFx.lineCircleOut(backColor, 18f, 20, 2), NHFx.sharpBlast(backColor, frontColor, 120f, 40f));
							despawnHit = true;
						}
							
							@Override
							public void update(Bullet b){
								super.update(b);
								b.collided.clear();
							}
							
							@Override
							public void hit(Bullet b, float x, float y){
								super.hit(b, x, y);
								
								UltFire.createChance(x, y, 12, 0.15f, b.team);
							}
						};
						
						shootStatus = StatusEffects.slow;
						shootStatusDuration = bullet.lifetime * 1.5f;
					}}
			);
			
			weapons.add(
				copyAnd(basicCannon, weapon -> {
					weapon.x = 19.5f;
					weapon.y = -28;
					weapon.autoTarget = true;
					weapon.controllable = false;
				}), copyAnd(basicCannon, weapon -> {
					weapon.x = 10;
					weapon.y = -46f;
					weapon.autoTarget = true;
					weapon.controllable = false;
				}),
				copyAndMove(laserCannon, 0, -25.5f)
			);
		}
			@Override
			public void drawSoftShadow(Unit unit){
				float z = Draw.z();
				Draw.z(z - 0.01f);
				super.drawSoftShadow(unit);
				Draw.z(z);
			}
			
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		declining = new NHUnitType("declining"){{
			outlineColor = OColor;
			weapons.add(copyAndMove(mainCannon, 0, -17));
			weapons.add(copyAndMove(mainCannon, 0, 25));
			weapons.add(copyAndMove(mainCannon, 0, -56));
			
			weapons.add(copyAndMove(pointDefenceWeaponC, 30, -30));
			weapons.add(copyAndMove(pointDefenceWeaponC, 36, -35));
			weapons.add(copyAndMove(pointDefenceWeaponC, 24, -35));
			
			weapons.add(laserCannon);
			
			immunities.addAll(StatusEffects.blasted, StatusEffects.tarred, StatusEffects.burning, StatusEffects.freezing, StatusEffects.melting, NHStatusEffects.ultFireBurn, NHStatusEffects.emp1);
			targetFlags = new BlockFlag[]{BlockFlag.unitAssembler, BlockFlag.turret, BlockFlag.reactor, BlockFlag.generator, null};
			
			health = 30000;
			speed = 0.75f;
			drag = 0.18f;
			hitSize = 60f;
			armor = 30;
			accel = 0.1f;
			rotateSpeed = 0.9f;
			buildSpeed = 6f;
			
			waveTrailX = 20;
			waveTrailY = -49;
			
			trailLength = 70;
			trailScl = 4f;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		sin = new NHUnitType("sin"){{
			outlineColor = OColor;
			abilities.add(new ForceFieldAbility(88.0F, 20F, 30000.0F, 900.0F), new StatusFieldAbility(NHStatusEffects.phased, 245f, 240f, 240f){{
				activeEffect = NHFx.lineSquareOut(NHColor.lightSkyBack, 60f, 240f, 4f, 45);
				applyEffect = NHFx.lineSquareOut(NHColor.lightSkyBack, 30f, 45f, 1f, 45);
			}});
			
			drawShields = false;
			engineOffset = 18.0F;
			engineSize = 9F;
			speed = 0.2f;
			hitSize = 52f;
			health = 80000f;
			buildSpeed = 4f;
			armor = 80f;
			
			ammoType = new ItemAmmoType(NHItems.presstanium);
			
			weapons.add(
					new Weapon(NewHorizon.name("sin-cannon")){{
						top = false;
						rotate = true;
						rotationLimit = 13f;
						rotateSpeed = 0.75f;
						alternate = true;
						shake = 3.5f;
						shootY = 32f;
						x = 42f;
						y = -2f;
						recoil = 3.4f;
						predictTarget = true;
						shootCone = 30f;
						reload = 60f;
						
						parts.add(new RegionPart("-shooter"){{
							under = turretShading = true;
							outline = true;
							mirror = false;
							moveY = -8f;
							progress = PartProgress.recoil;
						}});
						
						shoot = new ShootPattern(){{
							shots = 3;
							shotDelay = 3.5f;
						}};
						
						velocityRnd = 0.075f;
						inaccuracy = 6.0F;
						ejectEffect = Fx.none;
						bullet = new BasicBulletType(8, 200f, NHBullets.STRIKE){{
							trailColor = lightningColor = backColor = lightColor = NHColor.lightSkyBack;
							frontColor = NHColor.lightSkyFront;
							lightning = 2;
							lightningCone = 360;
							lightningLengthRand = lightningLength = 8;
							homingPower = 0;
							scaleLife = true;
							collides = false;
							
							trailLength = 15;
							trailWidth = 3.5f;
							
							splashDamage = lightningDamage = damage;
							splashDamageRadius = 48f;
							lifetime = 95f;
							
							width = 22f;
							height = 35f;
							
							trailEffect = NHFx.trailToGray;
							trailParam = 3f;
							trailChance = 0.35f;
							
							hitShake = 7f;
							hitSound = Sounds.explosion;
							hitEffect = NHFx.hitSpark(backColor, 75f, 24, 95f, 2.8f, 16);
							
							smokeEffect = new OptionalMultiEffect(NHFx.hugeSmokeGray, NHFx.circleSplash(backColor, 60f, 8, 60f, 6));
							shootEffect = NHFx.hitSpark(backColor, 30f, 15, 35f, 1.7f, 8);
							
							despawnEffect = NHFx.blast(backColor, 60);
							
							fragBullet = NHBullets.basicSkyFrag;
							fragBullets = 5;
							fragLifeMax = 0.6f;
							fragLifeMin = 0.2f;
							fragVelocityMax = 0.35f;
							fragVelocityMin = 0.074f;
						}
							
							@Override
							public void hit(Bullet b, float x, float y){
								super.hit(b, x, y);
								UltFire.createChance(b, splashDamageRadius, 0.4f);
							}
						};
						
						shootSound = Sounds.artillery;
					}},
					new Weapon(){{
						mirror = false;
						rotate = true;
						rotateSpeed = 25f;
						x = 0;
						y = 12f;
						recoil = 2.7f;
						shootY = 7f;
						shootCone = 40f;
						velocityRnd = 0.075f;
						reload = 150f;
						xRand = 18f;
						
						shoot = new ShootSine(){{
							shots = 12;
							shotDelay = 4f;
						}};
						
						
						inaccuracy = 5.0F;
						ejectEffect = Fx.none;
						bullet = NHBullets.annMissile;
						shootSound = NHSounds.launch;
					}}
			);
			
			weapons.add(copyAndMove(multipleLauncher, 26, -12.5f));
			
			weapons.add(copyAnd(laserCannon, w -> {
				w.mirror = false;
				w.x = 0;
				w.y = 14;
			}));
			
			weapons.add(copyAndMove(pointDefenceWeaponC, 22, 18f));
			weapons.add(copyAndMove(pointDefenceWeaponC, 25, 2));
			
			immunities.addAll(NHStatusEffects.scannerDown, NHStatusEffects.weak, NHStatusEffects.emp1, NHStatusEffects.emp2, NHStatusEffects.emp3, NHStatusEffects.scrambler, StatusEffects.disarmed, StatusEffects.melting, StatusEffects.burning, StatusEffects.wet, StatusEffects.shocked, StatusEffects.tarred, StatusEffects.muddy, StatusEffects.slow, StatusEffects.disarmed);
			
			groundLayer = Layer.legUnit + 0.1f;
			
			mechLandShake = 12f;
			stepShake = 5f;
			
			rotateSpeed = 1f;
			fallSpeed = 0.03f;
			mechStepParticles = true;
			canDrown = false;
			mechFrontSway = 2.2f;
			mechSideSway = 0.8f;
			canBoost = true;
			boostMultiplier = 2.5f;
		}
//			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this); NHPixmap.outlineLegs(packer, this);}
		};
		
		anvil = new NHUnitType("anvil"){{
			outlineColor = OColor;
			constructor = EntityMapping.map(3);

			EnergyFieldAbility ability = new EnergyFieldAbility(150f, 150f, 300f);
			ability.color = NHColor.thurmixRed;
			ability.y = -9f;
			ability.healEffect = new Effect(11, e -> {
				Draw.color(NHColor.thurmixRed);
				Lines.stroke(e.fout() * 2f);
				Lines.circle(e.x, e.y, 2f + e.finpow() * 7f);
			});
			ability.status = NHStatusEffects.emp1;
			ability.sectors = 4;
			ability.sectorRad = 0.16f;
			ability.healPercent = 0.25f;
			ability.statusDuration = 120f;
			ability.shootSound = NHSounds.synchro;

			abilities.add(ability);

			immunities.addAll(NHStatusEffects.scannerDown, StatusEffects.slow, StatusEffects.electrified, StatusEffects.muddy, StatusEffects.blasted, StatusEffects.shocked, StatusEffects.sapped, NHStatusEffects.emp1, NHStatusEffects.emp2, NHStatusEffects.weak);

			hitSize = 70f;
			armor = 42;
			health = 64000.0F;
			speed = 1F;
			rotateSpeed = 0.75f;
			accel = 0.06F;
			drag = 0.035f;
			flying = true;
			engineOffset = 65.0F;
			engineSize = 24f;
			buildSpeed = 5.0F;
			drawShields = true;
			lowAltitude = true;
			buildBeamOffset = 63.0F;
			payloadCapacity = (3 * 3) * tilePayload;

			Weapon weapon = new Weapon(NewHorizon.name("anvil-cannon")){{
				mirror = true;
				rotate = true;
				
				rotateSpeed = 1.25f;
				rotationLimit = 35f;
				
				alternate = true;
				top = true;

				shootY = 31;
				recoil = 1f;
				recoilTime = 90f;
				reload = 160f;
				shootCone = 15f;
				
				layerOffset = 0.003f;
				
				parts.add(new RegionPart("-shooter"){{
					moveY = -7f;
					progress = PartProgress.recoil;
					under = turretShading = outline = true;
				}});
				
				parts.add(new RegionPart("-panel"){{
					under = outline = true;
					x = 5;
					y = -0.25f;
					moveRot = -45;
					moveX = 4;
					moveY = -1f;
					layerOffset = -0.001f;
					progress = PartProgress.warmup.add(PartProgress.recoil.mul(0.3f));
				}});
				
				bullet = new ShieldBreakerType(9, 300f, 1200){{
					sprite = NHBullets.MISSILE_LARGE;
					trailColor = lightningColor = backColor = lightColor = NHColor.thurmixRed;
					frontColor = NHColor.thurmixRedLight;
					lightning = 2;
					lightningCone = 360;
					lightningLengthRand = lightningLength = 12;
					homingPower = 0;

					lifetime = 45f;
					trailLength = 15;
					trailWidth = 3.5f;

					splashDamage = lightningDamage = damage * 0.7f;
					splashDamageRadius = 40f;

					width = 22f;
					height = 35f;

					hitShake = despawnShake = 3f;
					hitSound = despawnSound = Sounds.explosion;
					hitEffect = new OptionalMultiEffect(NHFx.blast(backColor, 60f), NHFx.hitSpark(backColor, 75f, 8, 80f, 2f, 12f));

					smokeEffect = NHFx.hugeSmokeGray;
					shootEffect = NHFx.shootLineSmall(backColor);

					despawnEffect = NHFx.lightningHitLarge(backColor);

					status = NHStatusEffects.weak;
					statusDuration = 180f;
				}};

				shoot = new ShootPattern(){{
					shots = 3;
					shotDelay = 14f;
				}};
				
				inaccuracy = 3f;
				velocityRnd = 0.095f;

				x = 30;
				y = -2;

				shootSound = NHSounds.flak;
				shake = 4f;
			}};
			
			Weapon aaTurret = new Weapon(NewHorizon.name("rapid-laser-cannon")){{
				mirror = true;
				rotate = true;
				alternate = true;
				top = true;

				autoTarget = true;
				rotateSpeed = 30;
				shootY = 10;
				recoil = 2f;
				reload = 12f;

				heatColor = Pal.redderDust;
				cooldownTime = 45f;

				bullet = new ShrapnelBulletType(){{
					length = 520;
					damage = 300.0F;
					status = StatusEffects.slow;
					statusDuration = 60f;
					width = 11f;
					fromColor = NHColor.thurmixRedLight;
					hitColor = lightColor = lightningColor = toColor = NHColor.thurmixRed;
					shootEffect = NHFx.lightningHitSmall(toColor);
					smokeEffect = new OptionalMultiEffect(new Effect(lifetime + 2f, b -> {
						Draw.color(fromColor, toColor, b.fin());
						Fill.circle(b.x, b.y, (width / 2f) * b.fout());
						DrawFunc.tri(b.x, b.y, width / 1.75f * b.fout(Interp.circleIn), 30f, b.rotation + 60);
						DrawFunc.tri(b.x, b.y, width / 1.75f * b.fout(Interp.circleIn), 30f, b.rotation - 60);
					}), NHFx.hitSpark(toColor, 35f, 6, 24f, 1.75f, 8f));
				}};

				shoot = new ShootPattern();

				x = 20;
				y = -34;

				shootSound = NHSounds.synchro;
				shake = 1f;
			}};

			PointDefenseWeapon pointDefenseWeapon = new PointDefenseWeapon(NewHorizon.name("anvil-point-cannon")){{
				color = NHColor.thurmixRed;
				x = 14;
				y = -2f;
				reload = 6f;
				targetInterval = 6f;
				targetSwitchInterval = 8f;
				bullet = new BulletType(){{
					shootEffect = NHFx.shootLineSmall(color);
					hitEffect = NHFx.lightningHitSmall(color);
					maxRange = 280f;
					damage = 120f;
				}};
			}};

			weapons.addAll(
					weapon, copyAndMove(weapon, 50, -24), aaTurret, copyAndMove(aaTurret, 12, -12),
					pointDefenseWeapon, copyAndMove(pointDefenseWeapon, 13, 41)
			);

			range = 500f;

			ammoType = new PowerAmmoType();

			targetFlags = new BlockFlag[]{BlockFlag.factory, BlockFlag.turret, BlockFlag.reactor, BlockFlag.generator, BlockFlag.core, null};

		}
//			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}
		};
		
		guardian = new NHUnitType("guardian"){{
			clipSize = 260f;
			
			engineLayer = Layer.effect;
			engineOffset = 0;
			
			deathExplosionEffect = Fx.none;
			deathSound = Sounds.plasmaboom;
			trailLength = 40;
			trailScl = 3f;
			
			immunities = ObjectSet.with(NHStatusEffects.scannerDown, NHStatusEffects.weak, StatusEffects.wet, StatusEffects.shocked, StatusEffects.tarred, StatusEffects.burning,
					StatusEffects.melting, StatusEffects.blasted, StatusEffects.corroded, StatusEffects.electrified, StatusEffects.freezing, StatusEffects.muddy,
					NHStatusEffects.emp1, NHStatusEffects.emp2, NHStatusEffects.emp3, NHStatusEffects.quantization
			);
			
			armor = 20;
			
			hitSize = 45;
			speed = 1.5f;
			accel = 0.07F;
			drag = 0.075F;
			health = 30000;
			itemCapacity = 0;
			rotateSpeed = 6;
			engineSize = 8f;
			flying = true;
			
			trailLength = 70;
			buildSpeed = 10f;
			crashDamageMultiplier = Mathf.clamp(hitSize / 10f, 1, 10);
			payloadCapacity = Float.MAX_VALUE;
			buildBeamOffset = 0;
			
			weapons.add(new Weapon(){{
				reload = 180f;
				x = y =shootX = shootY = 0;
				shootCone = 360;
				shootSound = NHSounds.blaster;
				bullet = NHBullets.guardianBullet;
				shoot = new ShootPattern(){
					public void shoot(int totalShots, BulletHandler handler){
						for(int i = 0; i < shots; i++){
							handler.shoot(0, 0, Mathf.random(360), firstShotDelay + shotDelay * i);
						}
					}
					
					{
						shots = 15;
						shotDelay = 3f;
					}
				};
			}}, new Weapon(){{
				reload = 120f;
				x = y = shootX = shootY = 0;
				rotateSpeed = 360;
				
				shootSound = NHSounds.hugeShoot;
				
				velocityRnd = 0.015f;
				
				shoot = new ShootMulti(new ShootSummon(0, 0, 220, 0){{
					shots = 3;
					shotDelay = 1f;
				}}, new ShootSpread(){{
					shots = 2;
					spread = 2f;
					shotDelay = 25;
				}});
				
				bullet = NHBullets.guardianBulletLightningBall;
			}}, new Weapon() {
				{
					shootCone = 30.0F;
					predictTarget = false;
					top = false;
					mirror = false;
					rotate = true;
					x = y = shootX = shootY = 0.0F;
					continuous = false;
					rotateSpeed = 100.0F;
					reload = 600.0F;
					
					shootStatus = NHStatusEffects.intercepted;
					shootStatusDuration = 180f;
					
					shoot = new ShootSummon(0, 0, 180, 0){{
						shots = 3;
					}};
					
					shake = 13.0F;
					shootSound = Sounds.none;
					bullet = new StrafeLaser(300.0F){
						public void init(Bullet b) {
							super.init(b);
							Sounds.laserblast.at(b);
						}
						
						public void hit(Bullet b, float x, float y) {
							super.hit(b, x, y);
							if (b.owner instanceof Unit) {
								Unit from = (Unit)b.owner;
								if (from.dead || !from.isAdded() || from.healthf() > 0.99F) {
									return;
								}
								
								from.heal(this.damage / 20.0F);
								if (Vars.headless || !NHSetting.enableDetails()) {
									return;
								}
								
								PosLightning.createEffect(b, from, b.team.color, 2, Mathf.random(1.5F, 3.0F));
							}
							
						}
						
						public void draw(Bullet b) {
							Tmp.c1.set(b.team.color).lerp(Color.white, Mathf.absin(4.0F, 0.1F));
							super.draw(b);
							Draw.z(110.0F);
							float fout = b.fout(0.25F) * Mathf.curve(b.fin(), 0.0F, 0.125F);
							Draw.color(Tmp.c1);
							Fill.circle(b.x, b.y, this.width / 1.225F * fout);
							if (b.owner instanceof Unit) {
								Unit unit = (Unit)b.owner;
								if (!unit.dead) {
									Draw.z(100.0F);
									Lines.stroke((this.width / 3.0F + Mathf.absin(Time.time, 4.0F, 0.8F)) * fout);
									Lines.line(b.x, b.y, unit.x, unit.y, false);
								}
							}
							
							
							for(int i : Mathf.signs){
								DrawFunc.tri(b.x, b.y, 6.0F * fout, 10.0F + 50.0F * fout, Time.time * 1.5F + (float)(90 * i));
								DrawFunc.tri(b.x, b.y, 6.0F * fout, 20.0F + 60.0F * fout, Time.time * -1.0F + (float)(90 * i));
							}
							
							Draw.z(110.001F);
							Draw.color(b.team.color, Color.white, 0.25F);
							Fill.circle(b.x, b.y, this.width / 1.85F * fout);
							Draw.color(Color.black);
							Fill.circle(b.x, b.y, this.width / 2.155F * fout);
							Draw.z(100.0F);
							Draw.reset();
							float rotation = this.dataRot ? b.fdata : b.rotation() + this.getRotation(b);
							float maxRange = this.maxRange * fout;
							float realLength = NHFunc.findLaserLength(b, rotation, maxRange);
							Tmp.v1.trns(rotation, realLength);
							Tmp.v2.trns(rotation, 0.0F, this.width / 2.0F * fout);
							Tmp.v3.setZero();
							if (realLength < maxRange) {
								Tmp.v3.set(Tmp.v2).scl((maxRange - realLength) / maxRange);
							}
							
							Draw.color(Tmp.c1);
							Tmp.v2.scl(0.9F);
							Tmp.v3.scl(0.9F);
							Fill.quad(b.x - Tmp.v2.x, b.y - Tmp.v2.y, b.x + Tmp.v2.x, b.y + Tmp.v2.y, b.x + Tmp.v1.x + Tmp.v3.x, b.y + Tmp.v1.y + Tmp.v3.y, b.x + Tmp.v1.x - Tmp.v3.x, b.y + Tmp.v1.y - Tmp.v3.y);
							if (realLength < maxRange) {
								Fill.circle(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Tmp.v3.len());
							}
							
							Tmp.v2.scl(1.2F);
							Tmp.v3.scl(1.2F);
							Draw.alpha(0.5F);
							Fill.quad(b.x - Tmp.v2.x, b.y - Tmp.v2.y, b.x + Tmp.v2.x, b.y + Tmp.v2.y, b.x + Tmp.v1.x + Tmp.v3.x, b.y + Tmp.v1.y + Tmp.v3.y, b.x + Tmp.v1.x - Tmp.v3.x, b.y + Tmp.v1.y - Tmp.v3.y);
							if (realLength < maxRange) {
								Fill.circle(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Tmp.v3.len());
							}
							
							Draw.alpha(1.0F);
							Draw.color(Color.black);
//							Draw.color(Tmp.c2.set(Tmp.c1).lerp(Color.white, 0.35F));
							Draw.z(Draw.z() + 0.01f);
							Tmp.v2.scl(0.5F);
							Fill.quad(b.x - Tmp.v2.x, b.y - Tmp.v2.y, b.x + Tmp.v2.x, b.y + Tmp.v2.y, b.x + (Tmp.v1.x + Tmp.v3.x) / 3.0F, b.y + (Tmp.v1.y + Tmp.v3.y) / 3.0F, b.x + (Tmp.v1.x - Tmp.v3.x) / 3.0F, b.y + (Tmp.v1.y - Tmp.v3.y) / 3.0F);
							Drawf.light(b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, this.width * 1.5F, this.getColor(b), 0.7F);
							Draw.reset();
							Draw.z(Draw.z() - 0.01f);
						}
					};
				}
				
				@Override
				protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
//					if(!unit.isBoss())return;
					super.shoot(unit, mount, shootX, shootY, rotation);
					NHFx.crossSpinBlast.at(unit.x, unit.y, unit.rotation, unit.team.color, unit);
				}
				
//				@Override
//				public void update(Unit unit, WeaponMount mount){
//					if(unit.isBoss())super.update(unit, mount);
//				}
				
				/*public void update(Unit unit, WeaponMount mount) {
					boolean can = unit.canShoot();
					float lastReload = mount.reload;
					mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0.0F);
					mount.recoil = Mathf.approachDelta(mount.recoil, 0.0F, Math.abs(this.recoil) * unit.reloadMultiplier / this.recoilTime);
					float weaponRotation;
					float mountX;
					if (this.rotate && (mount.rotate || mount.shoot) && can) {
						weaponRotation = unit.x + Angles.trnsx(unit.rotation - 90.0F, this.x, this.y);
						mountX = unit.y + Angles.trnsy(unit.rotation - 90.0F, this.x, this.y);
						mount.targetRotation = Angles.angle(weaponRotation, mountX, mount.aimX, mount.aimY) - unit.rotation;
						mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, this.rotateSpeed * Time.delta);
					} else if (!this.rotate) {
						mount.rotation = 0.0F;
						mount.targetRotation = unit.angleTo(mount.aimX, mount.aimY);
					}
					
					weaponRotation = unit.rotation - 90.0F + (this.rotate ? mount.rotation : 0.0F);
					mountX = unit.x + Angles.trnsx(unit.rotation - 90.0F, this.x, this.y);
					float mountY = unit.y + Angles.trnsy(unit.rotation - 90.0F, this.x, this.y);
					float bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY);
					float bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY);
					float shootAngle = this.rotate ? weaponRotation + 90.0F : Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY) + (unit.rotation - unit.angleTo(mount.aimX, mount.aimY));
					if (this.continuous && mount.bullet != null) {
						if (mount.bullet.isAdded() && !(mount.bullet.time >= mount.bullet.lifetime) && mount.bullet.type == this.bullet) {
							mount.reload = this.reload;
							mount.recoil = this.recoil;
							unit.vel.add(Tmp.v1.trns(unit.rotation + 180.0F, mount.bullet.type.recoil));
							if (this.shootSound != Sounds.none && !Vars.headless) {
								if (mount.sound == null) {
									mount.sound = new SoundLoop(this.shootSound, 1.0F);
								}
								
								mount.sound.update(bulletX, bulletY, true);
							}
						} else {
							mount.bullet = null;
						}
					} else {
						mount.heat = Math.max(mount.heat - Time.delta * unit.reloadMultiplier / this.cooldownTime, 0.0F);
						if (mount.sound != null) {
							mount.sound.update(bulletX, bulletY, false);
						}
					}
					
					boolean wasFlipped = mount.side;
					if (this.otherSide != -1 && this.alternate && mount.side == this.flipSprite && mount.reload <= this.reload / 2.0F && lastReload > this.reload / 2.0F) {
						unit.mounts[this.otherSide].side = !unit.mounts[this.otherSide].side;
						mount.side = !mount.side;
					}
					
					if (mount.shoot && can && (!this.useAmmo || unit.ammo > 0.0F || !Vars.state.rules.unitAmmo || unit.team.rules().infiniteAmmo) && (!this.alternate || wasFlipped == this.flipSprite) && unit.vel.len() >= this.minShootVelocity && mount.reload <= 1.0E-4F && Angles.within(this.rotate ? mount.rotation : unit.rotation, mount.targetRotation, this.shootCone)) {
						this.shoot(unit, mount, bulletX, bulletY, mount.aimX, mount.aimY, mountX, mountY, shootAngle, Mathf.sign(this.x));
						mount.reload = this.reload;
						if (this.useAmmo) {
							--unit.ammo;
							if (unit.ammo < 0.0F) {
								unit.ammo = 0.0F;
							}
						}
					}
					
				}*/
			});
			
			aiController = SniperAI::new;
			targetFlags = new BlockFlag[]{BlockFlag.reactor, BlockFlag.generator, BlockFlag.turret, null};
		}
			public Effect slopeEffect = NHFx.boolSelector;
			
			public final float outerEyeScl = 0.25f;
			public final float innerEyeScl = 0.18f;
			
			/*
			 * [0] -> Length
			 * [1] -> Arrow Offset
			 * [2] -> Width
			 * [3] -> Rotate Speed
			 * [4] -> Origin Offset
			 * */
			public final float[][] rotator = {
					{75f, 0, 8.5f, 1.35f, 0.1f},
					{55f, 0, 6.5f, -1.7f, 0.1f},
					{25, 0, 13, 0.75f, 0.3f},
					
					{100f, 33.5f, 11, 0.75f, 0.7f},
					{60f, -20, 6f, -0.5f, 1.25f}
			};
			
			
			public final float bodySize = 24f;
			
			@Override
			public void load(){
				super.load();
				shadowRegion = uiIcon = fullIcon = Core.atlas.find(NewHorizon.name("jump-gate-pointer"));
			}
			
			@Override
			public void init(){
				super.init();
				if(trailLength < 0)trailLength = (int)bodySize * 4;
				if(slopeEffect == NHFx.boolSelector)slopeEffect = new Effect(30, b -> {
					if(!(b.data instanceof Integer))return;
					int i = b.data();
					Draw.color(b.color);
					Angles.randLenVectors(b.id, (int)(b.rotation / 8f), b.rotation / 4f + b.rotation * 2f * b.fin(), (x, y) -> Fill.circle(b.x + x, b.y + y, b.fout() * b.rotation / 2.25f));
					Lines.stroke((i < 0 ? b.fin(Interp.pow2InInverse) : b.fout(Interp.pow2Out)) * 2f);
					Lines.circle(b.x, b.y, (i > 0 ? (b.fin(Interp.pow2InInverse) + 0.5f) : b.fout(Interp.pow2Out)) * b.rotation);
				}).layer(Layer.bullet);
				
				engineSize = bodySize / 4;
				engineSize *= -1;
			}
			
			@Override
			public void draw(Unit unit){
				super.draw(unit);
			}
			
			@Override
			public void drawBody(Unit unit){
				float sizeF = 1 + Mathf.absin(4f, 0.1f);
				Draw.z(Layer.flyingUnitLow + 5f);
				Draw.color(Color.black);
				Fill.circle(unit.x, unit.y, bodySize * sizeF * 0.71f * unit.healthf());
				
				Drawf.light(unit.x,unit.y, unit.hitSize * 4.05f, unit.team.color, 0.68f);
				Draw.z(Layer.effect + 0.001f);
				Draw.color(unit.team.color, Color.white, Mathf.absin(4f, 0.3f) + Mathf.clamp(unit.hitTime) / 5f * 3f);
				Draw.alpha(0.65f);
				Fill.circle(unit.x, unit.y, bodySize * sizeF * 1.1f);
				Draw.alpha(1f);
				Fill.circle(unit.x, unit.y, bodySize * sizeF);
				
				for(float[] j : rotator){
					for(int i : Mathf.signs){
						float ang = Time.time * j[3] + 90 + 90 * i + Mathf.randomSeed(unit.id, 360);
						Tmp.v1.trns(ang, hitSize * j[4]).add(unit);
						DrawFunc.arrow(Tmp.v1.x, Tmp.v1.y, j[2], j[0], j[1], ang);
					}
				}
				
				Draw.color(Tmp.c1.set(unit.team.color).lerp(Color.white, 0.65f));
				Fill.circle(unit.x, unit.y, bodySize * sizeF * 0.75f * unit.healthf());
				Draw.color(Color.black);
				Fill.circle(unit.x, unit.y, bodySize * sizeF * 0.7f * unit.healthf());
				
				Draw.color(unit.team.color);
				Tmp.v1.set(unit.aimX, unit.aimY).sub(unit).nor().scl(bodySize * 0.15f);
				Fill.circle(Tmp.v1.x + unit.x, Tmp.v1.y + unit.y, bodySize * sizeF * outerEyeScl);
				Draw.color(unit.team.color, Color.white, Mathf.absin(4f, 0.3f) + 0.45f);
				Tmp.v1.setLength(bodySize * sizeF * (outerEyeScl - innerEyeScl));
				Fill.circle(Tmp.v1.x + unit.x, Tmp.v1.y + unit.y, bodySize * sizeF * innerEyeScl);
				Draw.reset();
			}
			
			@Override
			public void update(Unit unit){
				super.update(unit);
				if(Mathf.chanceDelta(0.1))for(int i : Mathf.signs)slopeEffect.at(unit.x + Mathf.range(bodySize), unit.y + Mathf.range(bodySize), bodySize, unit.team.color, i);
			}
			
			@Override
			public void drawCell(Unit unit){
			}
			
			@Override
			public void drawControl(Unit unit){
				Draw.z(Layer.effect + 0.001f);
				Draw.color(unit.team.color, Color.white, Mathf.absin(4f, 0.3f) +  Mathf.clamp(unit.hitTime) / 5f);
				for(int i = 0; i < 4; i++){
					float rotation = Time.time * 1.5f + i * 90;
					Tmp.v1.trns(rotation, bodySize * 1.5f).add(unit);
					Draw.rect(NHContent.arrowRegion, Tmp.v1.x, Tmp.v1.y, rotation + 90);
				}
				Draw.reset();
			}
			
			@Override
			public void drawItems(Unit unit){
				super.drawItems(unit);
			}
			
			@Override
			public <T extends Unit & Legsc> void drawLegs(T unit){
			}
			
			@Override
			public void drawLight(Unit unit){
				Drawf.light(unit.x, unit.y, bodySize * 3f, unit.team.color, lightOpacity);
			}
			
			@Override
			public void drawMech(Mechc mech){
			}
			
			@Override
			public void drawOutline(Unit unit){
			}
			
			@Override
			public void drawEngines(Unit unit){
			}
			
			@Override
			public void drawTrail(Unit unit){
			}
			
			@Override
			public <T extends Unit & Payloadc> void drawPayload(T unit){
				super.drawPayload(unit);
			}
			
			@Override
			public void drawShadow(Unit unit){
			}
			
			@Override
			public void drawShield(Unit unit){
				float alpha = unit.shieldAlpha();
				float radius = unit.hitSize() * 1.3f;
				Fill.light(unit.x, unit.y, Lines.circleVertices(radius), radius, Tmp.c1.set(Pal.shield), Tmp.c2.set(unit.team.color).a(0.7f).lerp(Color.white, Mathf.clamp(unit.hitTime() / 2f)).a(Pal.shield.a * alpha));
			}
			
			@Override
			public void drawSoftShadow(Unit unit){
			}
			
			@Override
			public void drawWeapons(Unit unit){
			}
		};
		
		collapser = new NHUnitType("collapser"){{
			outlineColor = OColor;
			abilities.add(new ForceFieldAbility(180f, 60, 80000, 900));
			constructor = EntityMapping.map(3);

			
			immunise(this);
			immunities.remove(NHStatusEffects.end);
			fallSpeed = 0.008f;
			drawShields = false;

			immunities = ObjectSet.with(NHStatusEffects.weak, NHStatusEffects.emp2, NHStatusEffects.emp3, NHStatusEffects.emp1, NHStatusEffects.scrambler, NHStatusEffects.scannerDown, NHStatusEffects.ultFireBurn, StatusEffects.melting, StatusEffects.burning, StatusEffects.shocked, StatusEffects.electrified);

			deathExplosionEffect = new OptionalMultiEffect(NHFx.blast(NHColor.thurmixRed, 400f), new Effect(300F, 1600f, e -> {
				Rand rand = NHFunc.rand;
				float rad = 150f;
				rand.setSeed(e.id);

				Draw.color(Color.white, NHColor.thurmixRed, e.fin() + 0.6f);
				float circleRad = e.fin(Interp.circleOut) * rad * 4f;
				Lines.stroke(12 * e.fout());
				Lines.circle(e.x, e.y, circleRad);
				for(int i = 0; i < 16; i++){
					Tmp.v1.set(1, 0).setToRandomDirection(rand).scl(circleRad);
					DrawFunc.tri(e.x + Tmp.v1.x, e.y + Tmp.v1.y, rand.random(circleRad / 16, circleRad / 12) * e.fout(), rand.random(circleRad / 4, circleRad / 1.5f) * (1 + e.fin()) / 2, Tmp.v1.angle() - 180);
				}

				e.scaled(120f, i -> {
					Draw.color(Color.white, NHColor.thurmixRed, i.fin() + 0.4f);
					Fill.circle(i.x, i.y, rad * i.fout());
					Lines.stroke(18 * i.fout());
					Lines.circle(i.x, i.y, i.fin(Interp.circleOut) * rad * 1.2f);
					Angles.randLenVectors(i.id, 40, rad / 3, rad * i.fin(Interp.pow2Out), (x, y) -> {
						lineAngle(i.x + x, i.y + y, Mathf.angle(x, y), i.fslope() * 25 + 10);
					});

					if(NHSetting.enableDetails())Angles.randLenVectors(i.id, (int)(rad / 4), rad / 6, rad * (1 + i.fout(Interp.circleOut)) / 1.5f, (x, y) -> {
						float angle = Mathf.angle(x, y);
						float width = i.foutpowdown() * rand.random(rad / 6, rad / 3);
						float length = rand.random(rad / 2, rad * 5) * i.fout(Interp.circleOut);

						Draw.color(NHColor.thurmixRed);
						DrawFunc.tri(i.x + x, i.y + y, width, rad / 3 * i.fout(Interp.circleOut), angle - 180);
						DrawFunc.tri(i.x + x, i.y + y, width, length, angle);

						Draw.color(Color.black);

						width *= i.fout();

						DrawFunc.tri(i.x + x, i.y + y, width / 2, rad / 3 * i.fout(Interp.circleOut) * 0.9f * i.fout(), angle - 180);
						DrawFunc.tri(i.x + x, i.y + y, width / 2, length / 1.5f * i.fout(), angle);
					});

					Draw.color(Color.black);
					Fill.circle(i.x, i.y, rad * i.fout() * 0.75f);
				});

				Drawf.light(e.x, e.y, rad * e.fslope() * 4f, NHColor.thurmixRed, 0.7f);
			}).layer(Layer.effect + 0.001f));
			fallEffect = NHFx.blast(NHColor.thurmixRed, 120f);

			targetAir = targetGround = true;
			weapons.addAll(
					copyAndMove(collapserCannon, 60, -50),
					copyAndMove(collapserCannon, 40, -20),
					copyAndMove(collapserCannon, 32, 60),
					new Weapon(NewHorizon.name("collapser-laser")){{
						y = -42;
						x = 0;
						shootY = 25f;
						
						shoot = new ShootSpread(){{
							shots = 3;
							shotDelay = 15;
							spread = 3f;
						}};
						
						inaccuracy = 3f;
						reload = 150f;
						rotateSpeed = 1.5f;
						rotate = true;
						top = true;
						shootSound = NHSounds.flak;
						mirror = alternate = false;
						bullet = NHBullets.collapserBullet;
					}},
					new Weapon(){
						float rangeWeapon = 520f;

						@Override
						public void draw(Unit unit, WeaponMount mount){
							float z = Draw.z();

							Tmp.v1.trns(unit.rotation, y);
							float f = 1 - mount.reload / reload;
							float rad = 12f;

							float f1 = Mathf.curve(f,  0.4f, 1f);
							Draw.z(Layer.bullet);
							Draw.color(heatColor);
							for(int i : Mathf.signs){
								for(int j : Mathf.signs){
									DrawFunc.tri(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, f1 * rad / 3f + Mathf.num(j > 0) * 2f * (f1 + 1) / 2, (rad * 3f + Mathf.num(j > 0) * 20f) * f1, j * Time.time + 90 * i);
								}
							}

							TextureRegion arrowRegion = NHContent.arrowRegion;

							Tmp.v6.set(mount.aimX, mount.aimY).sub(unit);
							Tmp.v2.set(mount.aimX, mount.aimY).sub(unit).nor().scl(Math.min(Tmp.v6.len(), rangeWeapon)).add(unit);

							for (int l = 0; l < 4; l++) {
								float angle = 45 + 90 * l;
								for (int i = 0; i < 4; i++) {
									Tmp.v3.trns(angle, (i - 4) * tilesize + tilesize).add(Tmp.v2);
									float fS = (100 - (Time.time + 25 * i) % 100) / 100 * f1 / 4;
									Draw.rect(arrowRegion, Tmp.v3.x, Tmp.v3.y, arrowRegion.width * fS, arrowRegion.height * fS, angle + 90);
								}
							}

							Lines.stroke((1.5f + Mathf.absin( Time.time + 4, 8f, 1.5f)) * f1, heatColor);
							Lines.square(Tmp.v2.x, Tmp.v2.y, 4 + Mathf.absin(8f, 4f), 45);

							Lines.stroke(rad / 2.5f * mount.heat, heatColor);
							Lines.circle(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, rad * 2 * (1 - mount.heat));

							Draw.color(heatColor);
							Fill.circle(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, f * rad);
							Lines.stroke(f * 1.5f);
							DrawFunc.circlePercentFlip(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, f * rad + 5, Time.time, 20f);
							Draw.color(Color.white);
							Fill.circle(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, f * rad * 0.7f);

							Draw.z(z);
						}
						
						@Override
						protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
							shootSound.at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));

							BulletType ammo = bullet;
							float lifeScl = ammo.scaleLife ? Mathf.clamp(Mathf.dst(shootX, shootY, mount.aimX, mount.aimY) / ammo.range) : 1f;

							Tmp.v6.set(mount.aimX, mount.aimY).sub(unit);
							Tmp.v1.set(mount.aimX, mount.aimY).sub(unit).nor().scl(Math.min(Tmp.v6.len(), rangeWeapon)).add(unit);

							Bullet b = bullet.create(unit, unit.team, Tmp.v1.x, Tmp.v1.y, 0);
							b.vel.setZero();
							b.set(Tmp.v1);
							unit.apply(shootStatus, shootStatusDuration);

							if(Vars.headless)return;
							Vec2 vec2 = new Vec2().trns(unit.rotation, y).add(unit);
							PosLightning.createEffect(vec2, b, NHColor.thurmixRed, 3, 2.5f);
							for(int i = 0; i < 5; i++){
								Time.run(i * 6f, () -> {
									NHFx.chainLightningFade.at(vec2.x, vec2.y, Mathf.random(8, 14), NHColor.thurmixRed, b);
								});
							}
							
							ammo.shootEffect.at(shootX, shootY, rotation);
							ammo.smokeEffect.at(shootX, shootY, rotation);
						}

						{
							y = 50;
							x = 0;
							shootY = 25f;
							shoot = new ShootPattern();
							reload = 900f;
							rotateSpeed = 100f;
							rotate = true;
							top = false;
							mirror = alternate = predictTarget = false;
							heatColor = NHColor.thurmixRed;
							shootSound = NHSounds.hugeShoot;
							bullet = new EffectBulletType(480f){
								@Override
								protected float calculateRange(){
									return rangeWeapon;
								}

								@Override
								public void despawned(Bullet b){
									super.despawned(b);

									Vec2 vec = new Vec2().set(b);

									float damageMulti = b.damageMultiplier();
									Team team = b.team;
									for(int i = 0; i < splashDamageRadius / (tilesize * 3.5f); i++){
										int finalI = i;
										Time.run(i * despawnEffect.lifetime / (splashDamageRadius / (tilesize * 2)), () -> {
											Damage.damage(team, vec.x, vec.y, tilesize * (finalI + 6), splashDamage * damageMulti, true);
										});
									}

									Units.nearby(team, vec.x, vec.y, splashDamageRadius * 2, u -> {
										u.heal((1 - u.healthf()) / 3 * u.maxHealth());
										u.apply(StatusEffects.overclock, 360f);
									});

									Units.nearbyEnemies(team, vec.x, vec.y, splashDamageRadius * 2, u -> {
										u.apply(NHStatusEffects.scannerDown, 600f);
									});

									if(!NHSetting.enableDetails())return;
									float rad = 120;
									float spacing = 2.5f;

									for(int k = 0; k < (despawnEffect.lifetime - NHFx.chainLightningFadeReversed.lifetime) / spacing; k++){
										Time.run(k * spacing, () -> {
											for(int j : Mathf.signs){
												Vec2 v = Tmp.v6.rnd(rad * 2 + Mathf.random(rad * 4)).add(vec);
												(j > 0 ? NHFx.chainLightningFade : NHFx.chainLightningFadeReversed).at(v.x, v.y, 12f, hitColor, vec);
											}
										});
									}
								}

								@Override
								public void update(Bullet b){
									float rad = 120;

									Effect.shake(8 * b.fin(), 6, b);

									if(b.timer(1, 12)){
										Seq<Teamc> entites = new Seq<>();

										Units.nearbyEnemies(b.team, b.x, b.y, rad * 2.5f * (1 + b.fin()) / 2, entites::add);

										Units.nearbyBuildings(b.x, b.y, rad * 2.5f * (1 + b.fin()) / 2, e -> {
											if(e.team != b.team)entites.add(e);
										});

										entites.shuffle();
										entites.truncate(15);

										for(Teamc e : entites){
											PosLightning.create(b, b.team, b, e, lightningColor, false, lightningDamage, 5 + Mathf.random(5), PosLightning.WIDTH, 1, p -> {
												NHFx.lightningHitSmall.at(p.getX(), p.getY(), 0, lightningColor);
											});
										}
									}

									if(NHSetting.enableDetails() && b.lifetime() - b.time() > NHFx.chainLightningFadeReversed.lifetime)for(int i = 0; i < 2; i++){
										if(Mathf.chanceDelta(0.2 * Mathf.curve(b.fin(), 0, 0.8f))){
											for(int j : Mathf.signs){
												Sounds.spark.at(b.x, b.y, 1f, 0.3f);
												Vec2 v = Tmp.v6.rnd(rad / 2 + Mathf.random(rad * 2) * (1 + Mathf.curve(b.fin(), 0, 0.9f)) / 1.5f).add(b);
												(j > 0 ? NHFx.chainLightningFade : NHFx.chainLightningFadeReversed).at(v.x, v.y, 12f, hitColor, b);
											}
										}
									}

									if(b.fin() > 0.05f && Mathf.chanceDelta(b.fin() * 0.3f + 0.02f)){
										NHSounds.blaster.at(b.x, b.y, 1f, 0.3f);
										Tmp.v1.rnd(rad / 4 * b.fin());
										NHFx.shuttleLerp.at(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Tmp.v1.angle(), hitColor, Mathf.random(rad, rad * 3f) * (Mathf.curve(b.fin(Interp.pow2In), 0, 0.7f) + 2) / 3);
									}
								}

								@Override
								public void draw(Bullet b){
									float fin = Mathf.curve(b.fin(), 0, 0.02f);
									float f = fin * Mathf.curve(b.fout(), 0f, 0.1f);
									float rad = 120;

									float circleF = (b.fout(Interp.pow2In) + 1) / 2;

									Draw.color(hitColor);
									Lines.stroke(rad / 20 * b.fin());
									Lines.circle(b.x, b.y, rad * b.fout(Interp.pow3In));
									Lines.circle(b.x, b.y, b.fin(Interp.circleOut) * rad * 3f * Mathf.curve(b.fout(), 0, 0.05f));

									Rand rand = NHFunc.rand;
									rand.setSeed(b.id);
									for(int i = 0; i < (int)(rad / 3); i++){
										Tmp.v1.trns(rand.random(360f) + rand.range(1f) * rad / 5 * b.fin(Interp.pow2Out), rad / 2.05f * circleF + rand.random(rad * (1 + b.fin(Interp.circleOut)) / 1.8f));
										float angle = Tmp.v1.angle();
										DrawFunc.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, (b.fin() + 1) / 2 * 28 + rand.random(0, 8), rad / 16 * (b.fin(Interp.exp5In) + 0.25f), angle);
										DrawFunc.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, (b.fin() + 1) / 2 * 12 + rand.random(0, 2), rad / 12 * (b.fin(Interp.exp5In) + 0.5f) / 1.2f, angle - 180);
									}

									Angles.randLenVectors(b.id + 1, (int)(rad / 3), rad / 4 * circleF, rad * (1 + b.fin(Interp.pow3Out)) / 3, (x, y) -> {
										float angle = Mathf.angle(x, y);
										DrawFunc.tri(b.x + x, b.y + y, rad / 8 * (1 + b.fout()) / 2.2f, (b.fout() * 3 + 1) / 3 * 25 + rand.random(4, 12) * (b.fout(Interp.circleOut) + 1) / 2, angle);
										DrawFunc.tri(b.x + x, b.y + y, rad / 8 * (1 + b.fout()) / 2.2f, (b.fout() * 3 + 1) / 3 * 9 + rand.random(0, 2) * (b.fin() + 1) / 2, angle - 180);
									});

									Drawf.light(b.x, b.y, rad * f * (b.fin() + 1) * 2, Draw.getColor(), 0.7f);

									Draw.z(Layer.effect + 0.001f);
									Draw.color(hitColor);
									Fill.circle(b.x, b.y, rad * fin * circleF / 2f);
									Draw.color(NHColor.thurmixRedDark);
									Fill.circle(b.x, b.y, rad * fin * circleF * 0.75f / 2f);

								}

								{
									hittable = false;
									collides = false;
									collidesTiles = collidesAir = collidesGround = true;
									speed = 100;

									despawnHit = true;
									keepVelocity = false;

									splashDamageRadius = 800f;
									splashDamage = 800f;

									lightningDamage = 200f;
									lightning = 36;
									lightningLength = 60;
									lightningLengthRand = 60;

									rangeWeapon = 400f;
									hitShake = despawnShake = 40f;
									drawSize = clipSize = 800f;
									hitColor = lightColor = trailColor = lightningColor = NHColor.thurmixRed;

									fragBullets = 22;
									fragBullet = NHBullets.collapserBullet;
									hitSound = NHSounds.hugeBlast;
									hitSoundVolume = 4f;

									fragLifeMax = 1.1f;
									fragLifeMin = 0.7f;
									fragVelocityMax = 0.6f;
									fragVelocityMin = 0.2f;

									status = NHStatusEffects.weak;
									statusDuration = 3000f;

									shootEffect = NHFx.lightningHitLarge(hitColor);

									hitEffect = NHFx.hitSpark(hitColor, 240f, 220, 900, 8, 27);
									despawnEffect = NHFx.collapserBulletExplode;
								}
							};
						}
					}
			);

			hitSize = 130f;

			speed = 0.5f;
			health = 180000;
			rotateSpeed = 0.65f;
			engineSize = 30f;
			buildSpeed = 10f;
			engineOffset = 62f;
			itemCapacity = 300;
			armor = 180;
			lowAltitude = true;
			flying = true;
		}
			@Override public void createIcons(MultiPacker packer){super.createIcons(packer); NHPixmap.createIcons(packer, this);}

			@Override
			public void drawCell(Unit unit){
				super.drawCell(unit);

				Draw.z(Layer.effect + 0.001f);

				Draw.color(unit.team.color, Color.white, Mathf.absin(4f, 0.3f));
				Lines.stroke(3f + Mathf.absin(10f, 0.55f));
				DrawFunc.circlePercent(unit.x, unit.y, unit.hitSize, unit.healthf(), 0);

				for(int i = 0; i < 4; i++){
					float rotation = Time.time * 1.5f + i * 90;
					Tmp.v1.trns(rotation, hitSize * 1.1f).add(unit);
					Draw.rect(NHContent.arrowRegion, Tmp.v1.x, Tmp.v1.y, rotation + 90);
				}

				Draw.z(Layer.flyingUnitLow);
				Draw.reset();
			}
		};
	}
}
