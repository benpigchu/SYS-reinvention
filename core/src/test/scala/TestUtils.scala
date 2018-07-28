object InstGen{
	private object Opcode{
		val LUI=0x37L
		val AUIPC=0x17L
		val OP_IMM=0x13L
		val OP=0x33L
	}
	private object Funct3{
		val ADD=0x0L
		val SUB=0x0L
		val SLL=0x1L
		val SLT=0x2L
		val SLTU=0x3L
		val XOR=0x4L
		val SRL=0x5L
		val SRA=0x5L
		val OR=0x6L
		val AND=0x7L
	}
	private def addPart(origin:Long,value:Long,left:Long,right:Long=0L)={
		val width=left-right
		val i=(value>>right)&((1<<width)-1)
		(origin<<width)+i
	}
	private def U(rd:Long,imm:Long,opcode:Long)={
		var inst:Long=0
		inst=addPart(inst,imm,32,12)
		inst=addPart(inst,rd,5)
		inst=addPart(inst,opcode,7)
		inst
	}
	private def I(rd:Long,rs1:Long,imm:Long,funct3:Long,opcode:Long)={
		var inst:Long=0
		inst=addPart(inst,imm,12)
		inst=addPart(inst,rs1,5)
		inst=addPart(inst,funct3,3)
		inst=addPart(inst,rd,5)
		inst=addPart(inst,opcode,7)
		inst
	}
	private def R(rd:Long,rs1:Long,rs2:Long,funct7:Long,funct3:Long,opcode:Long)={
		var inst:Long=0
		inst=addPart(inst,funct7,7)
		inst=addPart(inst,rs2,5)
		inst=addPart(inst,rs1,5)
		inst=addPart(inst,funct3,3)
		inst=addPart(inst,rd,5)
		inst=addPart(inst,opcode,7)
		inst
	}
	def LUI(rd:Long,imm:Long)=U(rd,imm,Opcode.LUI)
	def AUIPC(rd:Long,imm:Long)=U(rd,imm,Opcode.AUIPC)
	def ADDI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,imm,Funct3.ADD,Opcode.OP_IMM)
	def SLTI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,imm,Funct3.SLT,Opcode.OP_IMM)
	def SLTIU(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,imm,Funct3.SLTU,Opcode.OP_IMM)
	def XORI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,imm,Funct3.XOR,Opcode.OP_IMM)
	def ORI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,imm,Funct3.OR,Opcode.OP_IMM)
	def ANDI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,imm,Funct3.AND,Opcode.OP_IMM)
	def SLLI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,0x0L+(imm&0x1FL),Funct3.SLL,Opcode.OP_IMM)
	def SRLI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,0x0L+(imm&0x1FL),Funct3.SRL,Opcode.OP_IMM)
	def SRAI(rd:Long,rs1:Long,imm:Long)=I(rd,rs1,0x400L+(imm&0x1FL),Funct3.SRA,Opcode.OP_IMM)
	def ADD(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.ADD,Opcode.OP)
	def SUB(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x20L,Funct3.SUB,Opcode.OP)
	def SLT(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.SLT,Opcode.OP)
	def SLTU(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.SLTU,Opcode.OP)
	def AND(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.AND,Opcode.OP)
	def OR(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.OR,Opcode.OP)
	def XOR(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.XOR,Opcode.OP)
	def SLL(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.SLL,Opcode.OP)
	def SRL(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x0L,Funct3.SRL,Opcode.OP)
	def SRA(rd:Long,rs1:Long,rs2:Long)=R(rd,rs1,rs2,0x20L,Funct3.SRA,Opcode.OP)
	def NOP=ADDI(0,0,0)
}