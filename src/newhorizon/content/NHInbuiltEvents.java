package newhorizon.content;

import arc.Core;
import arc.Events;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.entities.pattern.ShootMulti;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.pattern.ShootSpread;
import mindustry.entities.pattern.ShootSummon;
import mindustry.game.EventType;
import mindustry.graphics.Pal;
import newhorizon.NHGroups;
import newhorizon.NewHorizon;
import newhorizon.expand.entities.WorldEvent;
import newhorizon.expand.eventsys.*;
import newhorizon.util.annotation.ClientDisabled;
import newhorizon.util.struct.OV_Pair;

public class NHInbuiltEvents{
	public static final String applyKey = "applyTriggers";
	
	public static WorldEventType
		intervention_std;
	
	@ClientDisabled
	public static final Seq<AutoEventTrigger> autoTriggers = new Seq<>(), campaignTriggers = new Seq<>();
	
	private static void loadEventTriggers(){
		AutoEventTrigger wave1 = new AutoEventTrigger(){{
			disposable = triggerAfterAdd = true;
			reload = spacing = 10000;
			spacingBase = spacingRand = 0;
			items = OV_Pair.seqWith();
			units = OV_Pair.seqWith();
			buildings = OV_Pair.seqWith();
			minTriggerWave = 0;
			eventType = WorldEventType.inbuilt(new ReachWaveEvent("inbuilt-wave-10"){{
				targetWave = 10;
				toTrigger = new InterventionEventType("wave10-reward"){{
					spawn(NHUnitTypes.rhino, 1, NHUnitTypes.gather, 2);
					removeAfterTrigger = true;
					defaultTeam = () -> Vars.state.rules.defaultTeam;
				}
					@Override public Position target(WorldEvent e){
						Position p = defaultTeam.get().core();
						return p == null ? new Vec2().set(Vars.world.unitWidth() / 2f, Vars.world.unitHeight() / 2f) : p;
					}
					
					@Override
					public Position source(WorldEvent event){
						Position t = target(event);
						Tmp.v1.set(t).sub(Vars.spawner.getFirstSpawn()).nor().add(t);
						return Tmp.v1;
					}
				};
			}});
		}};
		
		AutoEventTrigger wave2 = new AutoEventTrigger(){{
			disposable = triggerAfterAdd = true;
			reload = spacing = 10000;
			spacingBase = spacingRand = 0;
			items = OV_Pair.seqWith();
			units = OV_Pair.seqWith();
			buildings = OV_Pair.seqWith();
			minTriggerWave = 0;
			eventType = WorldEventType.inbuilt(new ReachWaveEvent("inbuilt-wave-55"){{
				targetWave = 55;
				toTrigger = new InterventionEventType("wave55-reward"){{
					spawn(NHUnitTypes.saviour, 1, NHUnitTypes.naxos, 2);
					removeAfterTrigger = true;
					defaultTeam = () -> Vars.state.rules.defaultTeam;
				}
					@Override public Position target(WorldEvent e){
						Position p = defaultTeam.get().core();
						return p == null ? new Vec2().set(Vars.world.unitWidth() / 2f, Vars.world.unitHeight() / 2f) : p;
					}
					
					@Override
					public Position source(WorldEvent event){
						Position t = target(event);
						Tmp.v1.set(t).sub(Vars.spawner.getFirstSpawn()).nor().add(t);
						return Tmp.v1;
					}
				};
			}});
		}};
		
		autoTriggers.add(wave1, wave2);
		campaignTriggers.add(wave1, wave2);
		
		autoTriggers.addAll(new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.metalOxhydrigen, 1000, NHItems.presstanium, 1000);
			units = OV_Pair.seqWith(NHUnitTypes.gather, 5);
			eventType = WorldEventType.inbuilt(new WeatherEvent("inbuilt-weather-sun-storm", NHWeathers.solarStorm, Pal.ammo));
			
			minTriggerWave = 0;
			spacingBase = 900 * 60;
			spacingRand = 600 * 60;
		}},new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.seniorProcessor, 1000, NHItems.irayrondPanel, 1000);
			buildings = OV_Pair.seqWith(NHBlocks.jumpGate, 1);
			eventType = WorldEventType.inbuilt(new WeatherEvent("inbuilt-weather-quantum-storm", NHWeathers.quantumStorm, NHColor.darkEnrColor));
			
			minTriggerWave = 0;
			spacingBase = 1500 * 60;
			spacingRand = 600 * 60;
		}},new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.seniorProcessor, 1200, NHItems.presstanium, 5000, NHItems.upgradeSort, 500);
			eventType = WorldEventType.inbuilt(new RaidEventType("inbuilt-raid-artillery"){{
				ShootPattern shootPattern = new ShootMulti(new ShootSummon(0, 0, 30, 0){{
					shots = 8;
					shotDelay = 18f;
				}}, new ShootSpread(){{
					shots = 6;
					spread = 3f;
					shotDelay = 4f;
				}});
				
				ammo(NHBullets.declineProjectile, shootPattern);
				radius = 230;
				reloadTime = 20 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 1800 * 60;
			spacingRand = 600 * 60;
		}},new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.multipleSteel, 1500, NHItems.presstanium, 1000, Items.plastanium, 1000);
			eventType = WorldEventType.inbuilt(new RaidEventType("inbuilt-raid-std"){{
				ShootPattern shootPattern = new ShootMulti(new ShootSummon(0, 0, 30, 0){{
					shots = 8;
					shotDelay = 18f;
				}}, new ShootSpread(){{
					shots = 30;
					spread = 8f;
					shotDelay = 4f;
				}});
				
				ammo(NHBullets.synchroFusion, shootPattern, NHBullets.synchroThermoPst, shootPattern);
				radius = 230;
				reloadTime = 300 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 1200 * 60;
			spacingRand = 300 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.setonAlloy, 2000, NHItems.darkEnergy, 2000);
			eventType = WorldEventType.inbuilt(new RaidEventType("inbuilt-raid-raid"){{
				ammo(NHBullets.airRaidBomb, new ShootMulti(new ShootSummon(0, 0, 220, 0){{
					shots = 6;
					shotDelay = 18f;
				}}, new ShootSpread(){{
					shots = 12;
					spread = 8f;
					shotDelay = 4;
				}}));
				reloadTime = 420 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 2000 * 60;
			spacingRand = 600 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.irayrondPanel, 1500, NHItems.presstanium, 3000, Items.phaseFabric, 100);
			eventType = WorldEventType.inbuilt(new RaidEventType("inbuilt-raid-sav"){{
				ShootPattern shootPattern = new ShootMulti(new ShootSummon(0, 0, 40, 0){{
					shots = 8;
					shotDelay = 18f;
				}}, new ShootSpread(){{
					shots = 30;
					spread = 8f;
					shotDelay = 8f;
				}});
				
				ammo(NHBullets.saviourBullet, shootPattern);
				radius = 340;
				reloadTime = 300 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 1800 * 60;
			spacingRand = 120 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.darkEnergy, 1500, NHItems.upgradeSort, 3000);
			eventType = WorldEventType.inbuilt(new RaidEventType("inbuilt-raid-arc"){{
				ShootPattern shootPattern = new ShootMulti(new ShootSummon(0, 0, 40, 0){{
					shots = 4;
					shotDelay = 18f;
				}}, new ShootSpread(){{
					shots = 1;
					spread = 8f;
				}});
				
				ammo(NHBullets.arc_9000, shootPattern);
				radius = 200;
				reloadTime = 300 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 2400 * 60;
			spacingRand = 600 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.setonAlloy, 1200, NHItems.multipleSteel, 3000);
			buildings = OV_Pair.seqWith(NHBlocks.jumpGate, 1);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-destruction"){{
				spawn(NHUnitTypes.destruction, 3, NHUnitTypes.naxos, 2);
				reloadTime = 30 * 60;
			}});
			
			spacingBase = 2400 * 60;
			spacingRand = 600 * 60;
			disposable = true;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.darkEnergy, 1000);
			buildings = OV_Pair.seqWith(NHBlocks.eternity, 2);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-pester-fleet"){{
				spawn(NHUnitTypes.pester, 1, NHUnitTypes.guardian, 5);
				reloadTime = 30 * 60;
			}});
			
			spacingBase = 60 * 60;
			spacingRand = 0;
			disposable = true;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.darkEnergy, 1000);
			units = OV_Pair.seqWith(NHUnitTypes.pester, 1);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-pester"){{
				spawn(NHUnitTypes.pester, 1);
				reloadTime = 30 * 60;
				
			}});
			spacingBase = 3600 * 60;
			spacingRand = 600 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(Items.plastanium, 1000, NHItems.metalOxhydrigen, 400);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-0"){{
				spawn(NHUnitTypes.warper, 8, NHUnitTypes.assaulter, 4, NHUnitTypes.branch, 4);
				reloadTime = 30 * 60;
				
			}});
			spacingBase = 480 * 60;
			spacingRand = 60 * 60;
		}}, new AutoEventTrigger(){{
			buildings = OV_Pair.seqWith(NHBlocks.jumpGate, 1);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-1"){{
				spawn(NHUnitTypes.naxos, 2, NHUnitTypes.branch, 4, NHUnitTypes.warper, 10, NHUnitTypes.assaulter, 4);
				reloadTime = 30 * 60;
			}});
			
			minTriggerWave = 25;
			spacingBase = 600 * 60;
			spacingRand = 300 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(Items.thorium, 50, NHItems.zeta, 80, NHItems.presstanium, 30);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-2"){{
				spawn(NHUnitTypes.branch, 4, NHUnitTypes.sharp, 4);
				reloadTime = 15 * 60;
			}});
			
			
			spacingBase = 180 * 60;
			spacingRand = 180 * 60;
		}}, new AutoEventTrigger(){{
			units = OV_Pair.seqWith(NHUnitTypes.guardian, 1);
			items = OV_Pair.seqWith(NHItems.darkEnergy, 2000);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-3"){{
				spawn(NHUnitTypes.guardian, 2);
				reloadTime = 45 * 60;
			}});
			
			minTriggerWave = 35;
			spacingBase = 1800 * 60;
			spacingRand = 120 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(NHItems.upgradeSort, 3000, NHItems.darkEnergy, 1000);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-4"){{
				spawn(NHUnitTypes.longinus, 4, NHUnitTypes.naxos, 10, NHUnitTypes.saviour, 2);
				reloadTime = 45 * 60;
			}});
			
			minTriggerWave = 35;
			spacingBase = 1200 * 60;
			spacingRand = 300 * 60;
		}}, new AutoEventTrigger(){{
			items = OV_Pair.seqWith(Items.graphite, 800, Items.silicon, 800, Items.thorium, 1000);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-5"){{
				spawn(UnitTypes.horizon, 20, NHUnitTypes.sharp, 6);
				reloadTime = 45 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 240 * 60;
			spacingRand = 60 * 60;
		}}, new AutoEventTrigger(){{
			buildings = OV_Pair.seqWith(NHBlocks.jumpGate, 1);
			items = OV_Pair.seqWith(NHItems.juniorProcessor, 800, NHItems.presstanium, 800, NHItems.multipleSteel, 400);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-6"){{
				spawn(NHUnitTypes.warper, 4, NHUnitTypes.sharp, 6);
				reloadTime = 120 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 360 * 60;
			spacingRand = 360 * 60;
		}}, new AutoEventTrigger(){{
			buildings = OV_Pair.seqWith(NHBlocks.jumpGate, 1);
			items = OV_Pair.seqWith(NHItems.seniorProcessor, 1200, NHItems.irayrondPanel, 800, NHItems.setonAlloy, 400, NHItems.upgradeSort, 200);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-7"){{
				spawn(NHUnitTypes.saviour, 2, NHUnitTypes.naxos, 2);
				reloadTime = 60 * 60;
			}});
			
			minTriggerWave = 0;
			spacingBase = 600 * 60;
			spacingRand = 240 * 60;
		}}, new AutoEventTrigger(){{
			buildings = OV_Pair.seqWith(NHBlocks.jumpGate, 1);
			items = OV_Pair.seqWith(NHItems.upgradeSort, 1200, NHItems.setonAlloy, 800, NHItems.seniorProcessor, 400);
			eventType = WorldEventType.inbuilt(new InterventionEventType("inbuilt-inbound-8"){{
				spawn(NHUnitTypes.anvil, 1);
				reloadTime = 60 * 60;
				
			}});
			
			minTriggerWave = 0;
			spacingBase = 600 * 60;
			spacingRand = 300 * 60;
		}});
		
			Events.on(EventType.WorldLoadEvent.class, e -> {
				if(Vars.state.isCampaign() && Vars.state.rules.sector.isCaptured()){
					NHGroups.events.clear();
					NHGroups.autoEventTrigger.clear();
					Vars.state.rules.tags.remove(applyKey);
					return;
				}
				
				if(Vars.state.rules.pvp || NHGroups.autoEventTrigger.size() >= autoTriggers.size)return;
				if(Vars.headless || NewHorizon.DEBUGGING){
					Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> {
						EventHandler.runEventOnce("setup-triggers", () -> {
							if(NHGroups.autoEventTrigger.isEmpty() && !Vars.state.rules.pvp){
								autoTriggers.each(t -> {
									t.copy().add();
									NewHorizon.debugLog(t.eventType.toString());
								});
							}
						});
					})));
				}
				
				if(Vars.state.isCampaign() && Vars.state.rules.sector.planet == NHPlanets.midantha){
					Core.app.post(() -> {
						if(Vars.state.isCampaign() && Vars.state.rules.tags.containsKey(applyKey) && !Vars.state.rules.sector.isCaptured()){
							if(NHGroups.autoEventTrigger.isEmpty() && !Vars.state.rules.pvp)autoTriggers.each(t -> t.copy().add());
							AutoEventTrigger.setScale(0.55f);
						}
					});
				}
			});
		
		Events.on(EventType.SectorCaptureEvent.class, e -> {
			NHGroups.events.clear();
		});
	}
	
	public static void load(){
		loadEventTriggers();
		
		intervention_std = new InterventionEventType("intervention_std"){{
			spawn(NHUnitTypes.branch, 5);
		}};
		
		WorldEventType.addInbuilt(intervention_std);
	}
}
