var p_type = argument0;
var xx = argument1;
var yy = argument2;
var dir = argument3;
var dir_range = argument4;
var num = argument5;

part_emitter_region(sys_particle.p_system_up, sys_particle.p_emitter_up, xx-3, xx+3, yy-3, yy+3, ps_shape_ellipse, ps_distr_gaussian);
part_type_direction(p_type, dir-dir_range, dir+dir_range, 0, 0);
part_type_orientation( p_type, dir, dir, 0, 0, 0 );
part_emitter_burst(sys_particle.p_system_up, sys_particle.p_emitter_up, p_type, num);
