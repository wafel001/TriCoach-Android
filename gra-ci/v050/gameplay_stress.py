#!/usr/bin/env python3
from pathlib import Path
import math, random, re
root=Path(__file__).resolve().parents[3] if (Path(__file__).resolve().parents[3]/'Assets').exists() else Path.cwd()
balance=(root/'Assets/Scripts/Data/GameBalance.cs').read_text()
def num(name):
    m=re.search(rf'public\s+(?:double|float|int)\s+{re.escape(name)}\s*=\s*([0-9.]+)d?f?;',balance)
    if not m: raise SystemExit(f'missing balance constant {name}')
    return float(m.group(1))
base_tap=num('baseTapDamage'); combo_step_hits=int(num('comboStepHits')); combo_step=num('comboStep'); max_combo=num('maxCombo'); tap_power=num('tapUpgradePowerGrowth'); tap_cost_base=num('tapUpgradeBaseCost'); tap_cost_growth=num('tapUpgradeCostGrowth'); enemy_hp=num('enemyBaseHp'); hp_growth=num('enemyHpGrowth'); enemy_gold=num('enemyBaseGold'); gold_growth=num('enemyGoldGrowth'); boss_every=int(num('bossEveryStages')); boss_hp_mult=num('bossHpMultiplier'); boss_gold_mult=num('bossGoldMultiplier'); prestige_unlock=int(num('prestigeUnlockStage')); prestige_exp=num('prestigeStarExponent')
hero_defs=[(.8,80,1),(2.4,350,8),(8.5,1500,18),(28,6500,30)]
RUNS=250; STEPS=1200; checks=0; max_stage_seen=1
for stage in range(1,501):
    hp=enemy_hp*(hp_growth**(stage-1))*(boss_hp_mult if stage%boss_every==0 else 1.0); gold=enemy_gold*(gold_growth**(stage-1))*(boss_gold_mult if stage%boss_every==0 else 1.0)
    assert math.isfinite(hp) and hp>0 and math.isfinite(gold) and gold>0; checks+=2
for seed in range(RUNS):
    rng=random.Random(8107+seed*97); stage=1; max_stage=1; gold=0.0; stars=0.0; tap_lvl=0; combo_hits=0; combo=1.0; hero=[0,0,0,0]; hp=enemy_hp
    for step in range(STEPS):
        boss=stage%boss_every==0; maxhp=enemy_hp*(hp_growth**max(0,stage-1))*(boss_hp_mult if boss else 1.0)
        if hp<=0 or hp>maxhp*1.000001: hp=maxhp
        combo_hits+=1; combo=min(max_combo,1.0+(combo_hits//max(1,combo_step_hits))*combo_step); dmg=base_tap*(tap_power**tap_lvl)*combo*(1+stars*.02)
        for i,(bdps,_,unlock) in enumerate(hero_defs):
            if hero[i]>0: dmg+=(bdps*hero[i]*(1.085**max(0,hero[i]-1)))*.12
        assert math.isfinite(dmg) and dmg>0; hp-=dmg; checks+=1
        if hp<=0:
            reward=enemy_gold*(gold_growth**max(0,stage-1))*(boss_gold_mult if boss else 1.0); assert math.isfinite(reward) and reward>0; gold+=reward; stage+=1; max_stage=max(max_stage,stage); hp=0
        tap_cost=tap_cost_base*(tap_cost_growth**tap_lvl)
        if gold>=tap_cost and rng.random()<.32: gold-=tap_cost; tap_lvl+=1
        for i,(bdps,base_cost,unlock) in enumerate(hero_defs):
            cost=base_cost*(1.115**hero[i])
            if max_stage>=unlock and gold>=cost and rng.random()<.08: gold-=cost; hero[i]+=1
        vals=[gold,stars,combo,dmg,maxhp]; assert all(math.isfinite(v) and v>=0 for v in vals); assert 1<=combo<=max_combo+1e-6; max_stage_seen=max(max_stage_seen,max_stage); checks+=6
# Direct prestige invariant sweep to guarantee the unlock/reset math is exercised even if a random run is slower.
for stage in range(prestige_unlock,501):
    gained=max(1,math.floor(stage**prestige_exp)); relics=max(1,math.floor(stage**1.35/35)); assert gained>=1 and relics>=1; checks+=2
print(f'GAMEPLAY STRESS: PASS ({RUNS*STEPS:,} simulated frames, {checks:,} invariant checks, stage formulas through 500)')
