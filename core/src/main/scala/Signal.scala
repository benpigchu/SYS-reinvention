package reinventation_core

import chisel3._

object BoolSignal{
	val X=false.B
	val T=true.B
	val F=false.B
}

object ALUASourceSignal{
	val width=2.W
	val A_X=0.U(width)
	val A_0=0.U(width)//zero add something->copy b
	val A_REGA=1.U(width)
	val A_PC=2.U(width)//aiupc,etc
}

object ALUBSourceSignal{
	val width=2.W
	val B_X=0.U(width)
	val B_0=0.U(width)//something add zero->copy a
	val B_REGB=1.U(width)
	val B_IMM=2.U(width)
}

object ImmTypeSignal{
	val width=3.W
	val IMM_X=0.U(width)
	val IMM_S=0.U(width)
	val IMM_B=1.U(width)
	val IMM_U=2.U(width)
	val IMM_J=3.U(width)
	val IMM_I=4.U(width)
}

object MemOpSignal{
	val width=1.W
	val M_X=0.U(width)
	val M_LOAD=0.U(width)
	val M_STORE=1.U(width)
}

object MemWidthSignal{
	val width=2.W
	val W_X=0.U(width)
	val W_B=0.U(width)
	val W_H=1.U(width)
	val W_W=2.U(width)
}

object WritebackSourceSignal{
	val width=1.W
	val WB_X=0.U(width)
	val WB_ALU=0.U(width)
	val WB_MEM=1.U(width)
}