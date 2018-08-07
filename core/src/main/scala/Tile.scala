package reinventation_core

import chisel3._
import chisel3.util._

class TileIO extends Bundle{
	val debug=new CoreDebugIO
	val mem=new RawMemIO
}

class Tile extends Module{
	val io=IO(new TileIO)
	val core=Module(new Core)
	val memadp=Module(new MemoryAdapter)
	core.io.mem<>memadp.io.core
	io.mem<>memadp.io.raw
	core.io.debug<>io.debug
}
