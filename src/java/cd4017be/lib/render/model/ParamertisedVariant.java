package cd4017be.lib.render.model;

import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Number;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.BlockModelRotation;

/**
 * 
 * @author cd4017be
 */
public class ParamertisedVariant implements ModelState {

	public static final ParamertisedVariant BASE = new ParamertisedVariant("model", null);

	public final BlockModelRotation orient;
	public String subModel;
	public final Parameters params;

	public ParamertisedVariant(BlockModelRotation orient, String name, Parameters param) {
		this.orient = orient;
		this.subModel = name;
		this.params = param;
	}

	public ParamertisedVariant(String name, Parameters param) {this(BlockModelRotation.X0_Y0, name, param);}
	public ParamertisedVariant(BlockModelRotation orient) {this(orient, "model", null);}

	public String splitPath() {
		String[] parts = subModel.split("\\.");
		String path = parts[0];
		if (parts.length > 1) path += "." + parts[1];
		if (parts.length > 2) subModel = parts[2];
		else subModel = "model";
		return path;
	}

	public boolean isBase() {
		return this == BASE || params == null && orient == BlockModelRotation.X0_Y0 && subModel.equals(BASE.subModel);
	}

	public static ParamertisedVariant parse(String name, BlockModelRotation orient) {
		int i = name.indexOf('(');
		Parameters params;
		if (i < 0) params = null;
		else {
			String arg = name.substring(i + 1, name.length() - (name.endsWith(")") ? 1 : 0));
			name = name.substring(0, i);
			if (arg.isEmpty()) params = new Parameters();
			else {
				String[] pars = arg.split(",");
				IOperand[] arr = new IOperand[pars.length];
				for (int j = 0; j < pars.length; j++)
					try {
						arr[j] = new Number(Double.parseDouble(pars[j]));
					} catch (NumberFormatException e) {
						arr[j] = new Text(pars[j]);
					}
				params = new Parameters(arr);
			}
		}
		return new ParamertisedVariant(orient, name, params);
	}

	public static ParamertisedVariant parse(String name) {
		int i = name.indexOf('#');
		BlockModelRotation orient;
		if (i < 0) orient = BlockModelRotation.X0_Y0;
		else {
			orient = BlockModelRotation.valueOf(name.substring(i+1).toUpperCase());
			if (orient == null) orient = BlockModelRotation.X0_Y0;
			name = name.substring(0, i);
		}
		return parse(name, orient);
	}

}
