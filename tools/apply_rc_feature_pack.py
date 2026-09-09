from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
def read(p):return (ROOT/p).read_text(encoding='utf-8')
def write(p,s):(ROOT/p).write_text(s,encoding='utf-8',newline='\n')
def rep(p,o,n):
 s=read(p);c=s.count(o)
 if c!=1:raise RuntimeError(f'{p}: expected 1 match got {c}: {o[:120]}')
 write(p,s.replace(o,n,1))
def rex(p,pat,n):
 s=read(p);out,c=re.subn(pat,n,s,count=1,flags=re.S)
 if c!=1:raise RuntimeError(f'{p}: regex expected 1 got {c}: {pat[:120]}')
 write(p,out)

# Menu editor preview and inspector use the same UISkin contract as runtime.
m='src/main/java/com/buttclapdev/twogamerl/studio/MenuEditorPane.java'
rex(m,r'    private Node buildButtonNode\(com\.buttclapdev\.twogamerl\.model\.GameProject\.MenuButton def\)\{.*?return MenuEffects\.decorate\(content,def\.animation,def\.animationSpeed,def\.hoverEffect\);\}',r'''    private Node buildButtonNode(com.buttclapdev.twogamerl.model.GameProject.MenuButton def){UISkinAsset skin=app.project().getUiSkins().get(def.uiSkinKey);String normal=skin!=null&&!skin.normalAssetKey.isBlank()?skin.normalAssetKey:def.assetKey,hover=skin!=null&&!skin.hoverAssetKey.isBlank()?skin.hoverAssetKey:def.hoverAssetKey,pressed=skin==null?"":skin.pressedAssetKey,disabled=skin==null?"":skin.disabledAssetKey;StackPane content=new StackPane();content.setPrefSize(def.width,def.height);content.setMinSize(def.width,def.height);content.setMaxSize(def.width,def.height);String initial=!def.enabled&&!disabled.isBlank()?disabled:normal;ImageView iv=initial.isBlank()?null:assetImageView(initial,def.width,def.height);if(iv!=null)content.getChildren().add(iv);else{java.awt.Color bg=skin==null?def.backgroundColor:skin.backgroundColor,border=skin==null?new java.awt.Color(75,106,132):skin.borderColor;double radius=skin==null?7:skin.radius,bw=skin==null?1:skin.borderWidth;content.setStyle(String.format(Locale.ROOT,"-fx-background-color:rgba(%d,%d,%d,%.3f);-fx-background-radius:%.2f;-fx-border-color:rgba(%d,%d,%d,%.3f);-fx-border-width:%.2f;-fx-border-radius:%.2f;",bg.getRed(),bg.getGreen(),bg.getBlue(),bg.getAlpha()/255.0,radius,border.getRed(),border.getGreen(),border.getBlue(),border.getAlpha()/255.0,bw,radius));}String fontKey=skin!=null&&!skin.fontKey.isBlank()?skin.fontKey:def.fontKey;java.awt.Color text=skin==null?def.textColor:(def.enabled?skin.textColor:skin.disabledTextColor);Label label=new Label(def.text);label.setFont(projectFont(fontKey,def.fontSize));label.setTextFill(fx(text));label.setMouseTransparent(true);content.getChildren().add(label);if(iv!=null&&def.enabled){if(!hover.isBlank()){content.setOnMouseEntered(e->configureImageView(iv,hover));content.setOnMouseExited(e->configureImageView(iv,normal));}if(!pressed.isBlank()){content.setOnMousePressed(e->configureImageView(iv,pressed));content.setOnMouseReleased(e->configureImageView(iv,hover.isBlank()?normal:hover));}}if(!def.enabled)content.setOpacity(.72);if(selection==Selection.BUTTON&&selectedButton==def)content.setStyle(content.getStyle()+";-fx-border-color:#52d3ff;-fx-border-width:2;");return MenuEffects.decorate(content,skin!=null&&skin.animation!=MenuAnimation.NONE?skin.animation:def.animation,def.animationSpeed,def.hoverEffect);}''')
# Add normal inspector controls for skin/enabled without forcing the dedicated lab.
rep(m,'ComboBox<String>sprite=assetPicker(b.assetKey),hoverSprite=assetPicker(b.hoverAssetKey);ColorPicker textColor=pickerColor(b.textColor),bgColor=pickerColor(b.backgroundColor);','ComboBox<String>sprite=assetPicker(b.assetKey),hoverSprite=assetPicker(b.hoverAssetKey);ComboBox<String>uiSkin=new ComboBox<>();uiSkin.getItems().add("(ninguno)");uiSkin.getItems().addAll(app.project().getUiSkins().keySet());uiSkin.setValue(b.uiSkinKey.isBlank()?"(ninguno)":b.uiSkinKey);CheckBox enabled=new CheckBox("Habilitado");enabled.setSelected(b.enabled);ColorPicker textColor=pickerColor(b.textColor),bgColor=pickerColor(b.backgroundColor);')
rep(m,'b.assetKey=assetValue(sprite);b.hoverAssetKey=assetValue(hoverSprite);b.textColor=awt(textColor.getValue());','b.assetKey=assetValue(sprite);b.hoverAssetKey=assetValue(hoverSprite);b.uiSkinKey="(ninguno)".equals(uiSkin.getValue())?"":Objects.toString(uiSkin.getValue(),"");b.enabled=enabled.isSelected();b.textColor=awt(textColor.getValue());')
rep(m,'sprite.setOnAction(e->apply.run());hoverSprite.setOnAction(e->apply.run());textColor.setOnAction(e->apply.run());','sprite.setOnAction(e->apply.run());hoverSprite.setOnAction(e->apply.run());uiSkin.setOnAction(e->apply.run());enabled.setOnAction(e->apply.run());textColor.setOnAction(e->apply.run());')
rep(m,'row(form,9,"Sprite",sprite);row(form,10,"Sprite hover",hoverSprite);row(form,11,"Acción",action);','row(form,9,"Sprite",sprite);row(form,10,"Sprite hover",hoverSprite);row(form,11,"UI Skin",uiSkin);row(form,12,"Estado",enabled);row(form,13,"Acción",action);')
rep(m,'row(form,12,"Destino",target);row(form,13,"Animación",animation);row(form,14,"Hover",hover);row(form,15,"Velocidad",speed);','row(form,14,"Destino",target);row(form,15,"Animación",animation);row(form,16,"Hover",hover);row(form,17,"Velocidad",speed);')

# Autocomplete knows bounded while and animator parameters.
a='src/main/java/com/buttclapdev/twogamerl/studio/ScriptAutocomplete.java'
rep(a,'"ifOther","chance","repeat","stop"','"ifOther","chance","while","repeat","stop"')
rep(a,'if(prefix.toLowerCase(Locale.ROOT).startsWith("self.")){for(String x:SELF_PATHS)add(out,"self."+x,"self."+x,"Ruta",p);return limit(out);}','if(prefix.toLowerCase(Locale.ROOT).startsWith("animator.param.")){for(AnimatorController c:app.project().getAnimatorControllers().values())for(String x:c.parameters.keySet())add(out,"Animator.param."+x,"Animator.param."+x,"Parámetro Animator",p);return limit(out);}if(prefix.toLowerCase(Locale.ROOT).startsWith("self.")){for(String x:SELF_PATHS)add(out,"self."+x,"self."+x,"Ruta",p);return limit(out);}')

# Runtime debug hooks used only by the RC smoke battery.
g='src/main/java/com/buttclapdev/twogamerl/runtime/GameView.java'
rep(g,'public String debugAnimationClip(String id){Body b=bodies.get(id);return b==null?null:b.animationClip;}public double debugRenderScale()','public String debugAnimationClip(String id){Body b=bodies.get(id);return b==null?null:b.animationClip;}public String debugAnimatorParameter(String id,String name){Body b=bodies.get(id);return b==null?null:b.animatorParameters.get(name);}public String debugAnimatorDirection(String id){Body b=bodies.get(id);return b==null?null:b.lastAnimationDirection;}public int debugFrameHitTargetCount(String id){Body b=bodies.get(id);return b==null?-1:b.frameHitTargets.size();}public double debugRenderScale()')

print('RC phase D integration polish staged')
