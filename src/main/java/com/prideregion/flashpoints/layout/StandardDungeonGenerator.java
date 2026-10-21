package com.prideregion.flashpoints.layout;

import java.util.*;

/**
 * Larger, seed-driven 16-room testing layout. Does not alter the v0.1 generator.
 * Gameplay timing is NOT established until Cobblemon encounters are implemented.
 */
public final class StandardDungeonGenerator {
    public static final int WIDTH=188, LENGTH=164, HEIGHT=14;
    private static final int ROWS=4, COLS=4;
    private StandardDungeonGenerator() {}

    public static DungeonLayout generate(long seed) {
        Random random = new Random(seed);
        int[][] cx = new int[ROWS][COLS], cz = new int[ROWS][COLS];
        boolean[][] walk = new boolean[LENGTH][WIDTH];
        final int bossId = 2*COLS+3;

        for (int r=0;r<ROWS;r++) for(int c=0;c<COLS;c++) {
            int px=23+c*46+random.nextInt(5)-2;
            int pz=21+r*39+random.nextInt(5)-2;
            cx[r][c]=px;cz[r][c]=pz;
            int rx=(r*COLS+c==bossId)?19:12+random.nextInt(5);
            int rz=(r*COLS+c==bossId)?18:12+random.nextInt(5);
            int shape=random.nextInt(3);
            for(int dz=-rz;dz<=rz;dz++) for(int dx=-rx;dx<=rx;dx++) {
                double a=Math.abs((double)dx/rx), b=Math.abs((double)dz/rz);
                boolean in=shape==0 ? a*a+b*b<=1
                    : shape==1 ? Math.max(a,b)+0.22*Math.min(a,b)<=1
                    : (Math.abs(dx)<=7 || Math.abs(dz)<=7) && a<1 && b<1;
                int x=px+dx,z=pz+dz;
                if(in && x>1 && z>1 && x<WIDTH-2 && z<LENGTH-2)walk[z][x]=true;
            }
        }

        // Random depth-first spanning tree; every room remains reachable.
        boolean[] seen=new boolean[ROWS*COLS];
        Deque<Integer> stack=new ArrayDeque<>();
        stack.push(COLS);seen[COLS]=true;
        Set<Long> edges=new TreeSet<>();
        while(!stack.isEmpty()) {
            int id=stack.peek(),x=id%COLS,z=id/COLS;
            ArrayList<Integer> neighbors=new ArrayList<>(4);
            if(x>0&&!seen[id-1])neighbors.add(id-1);
            if(x<COLS-1&&!seen[id+1])neighbors.add(id+1);
            if(z>0&&!seen[id-COLS])neighbors.add(id-COLS);
            if(z<ROWS-1&&!seen[id+COLS])neighbors.add(id+COLS);
            if(neighbors.isEmpty())stack.pop();
            else {
                int chosen=neighbors.get(random.nextInt(neighbors.size()));
                seen[chosen]=true;edges.add(edge(id,chosen));stack.push(chosen);
            }
        }
        ArrayList<Long> optional=new ArrayList<>();
        for(int id=0;id<ROWS*COLS;id++) {
            int x=id%COLS,z=id/COLS;
            if(x<COLS-1 && !edges.contains(edge(id,id+1)))optional.add(edge(id,id+1));
            if(z<ROWS-1 && !edges.contains(edge(id,id+COLS)))optional.add(edge(id,id+COLS));
        }
        Collections.shuffle(optional,random);
        for(int n=0;n<Math.min(1+random.nextInt(3),optional.size());n++)edges.add(optional.get(n));
        for(long e:edges) {
            int a=(int)(e>>>32),b=(int)e;
            int ax=cx[a/COLS][a%COLS],az=cz[a/COLS][a%COLS];
            int bx=cx[b/COLS][b%COLS],bz=cz[b/COLS][b%COLS];
            if(a/COLS==b/COLS) {
                int mx=(ax+bx)/2;
                corridor(walk,ax,az,mx,az);
                corridor(walk,mx,az,mx,bz);
                corridor(walk,mx,bz,bx,bz);
            } else {
                int mz=(az+bz)/2;
                corridor(walk,ax,az,ax,mz);
                corridor(walk,ax,mz,bx,mz);
                corridor(walk,bx,mz,bx,bz);
            }
        }

        byte[] blocks=new byte[WIDTH*LENGTH*HEIGHT];
        for(int z=0;z<LENGTH;z++) for(int x=0;x<WIDTH;x++) {
            if(walk[z][x]) {
                put(blocks,x,0,z,random.nextInt(10)==0?4:3);
                put(blocks,x,12,z,1);
                if((x+3*z)%47==0 && x>6 && z>6 && x<WIDTH-7 && z<LENGTH-7)
                    put(blocks,x,12,z,7);
            } else if(touches(walk,x,z)) {
                put(blocks,x,0,z,6);
                for(int y=1;y<=12;y++)put(blocks,x,y,z,random.nextInt(10)==0?2:1);
            }
        }
        for(int r=0;r<ROWS;r++)for(int c=0;c<COLS;c++) {
            int x=cx[r][c],z=cz[r][c],id=r*COLS+c;
            put(blocks,x,0,z,id==COLS?7:id==bossId?9:(id%4==0?8:5));
            put(blocks,x,12,z,7);
        }
        int bx=cx[2][3],bz=cz[2][3];
        for(int dx:new int[]{-7,7}) for(int dz:new int[]{-7,7})
            for(int y=1;y<=8;y++)put(blocks,bx+dx,y,bz+dz,10);
        DungeonLayout out=new DungeonLayout(seed,WIDTH,HEIGHT,LENGTH,
            cx[1][0],cz[1][0],bx,bz,blocks);
        if(!DungeonGenerator.reachable(out))
            throw new IllegalStateException("Unreachable boss: "+seed);
        return out;
    }

    private static long edge(int a,int b) {
        return ((long)Math.min(a,b)<<32)|(Math.max(a,b)&0xffffffffL);
    }
    private static void put(byte[] b,int x,int y,int z,int value) {
        b[(y*LENGTH+z)*WIDTH+x]=(byte)value;
    }
    private static void corridor(boolean[][] walk,int x1,int z1,int x2,int z2) {
        if(x1!=x2 && z1!=z2)throw new IllegalArgumentException("diagonal hall");
        int len=Math.max(Math.abs(x2-x1),Math.abs(z2-z1));
        for(int i=0;i<=len;i++) {
            int x=x1+Integer.signum(x2-x1)*i,z=z1+Integer.signum(z2-z1)*i;
            for(int dz=-4;dz<=4;dz++)for(int dx=-4;dx<=4;dx++) {
                int nx=x+dx,nz=z+dz;
                if(Math.abs(dx)+Math.abs(dz)<=6
                   && nx>1 && nz>1 && nx<WIDTH-2 && nz<LENGTH-2)
                    walk[nz][nx]=true;
            }
        }
    }
    private static boolean touches(boolean[][] walk,int x,int z) {
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
            int nx=x+dx,nz=z+dz;
            if(nx>=0&&nx<WIDTH&&nz>=0&&nz<LENGTH&&walk[nz][nx])return true;
        }
        return false;
    }
}
